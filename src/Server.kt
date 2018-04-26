import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.PrintStream
import java.io.BufferedOutputStream

class Server {
    enum class Method {
        OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT
    }

    private val mySocket = ServerSocket(5555)
    private lateinit var headerMap: MutableMap<String, String>
    private lateinit var resourcePath: String
    private lateinit var method: Method
    private lateinit var cookieMap: MutableMap<String, String>


    fun executavel(path: String, params: String? = null, writer: PrintStream) {
        val processBuilder: ProcessBuilder = ProcessBuilder()

        if(params == null) {
            processBuilder.command(path)
        } else {
            processBuilder.command(path, params)
        }

        val process = processBuilder.start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String? = reader.readLine()

        while(line != null) {
            writer.print(line)
            line = reader.readLine()
        }

        writer.flush()

    }

    private fun processHeader(reader: BufferedReader) {
        val firstLine = reader.readLine()
        var header = reader.readLine()

        while (reader.ready()) {
            header += "\n"
            header += reader.readLine()
        }

        this.headerMap = mutableMapOf()
        this.cookieMap = mutableMapOf()

        val hspt= header.split("\n")
        hspt.map { it: String -> it.split(": ") }
                .forEach { if (it.size > 1) {
                        if (it[0] == "Cookie") {
                            val sp1 = it[1].split(";")
                            sp1.map { it.split("=") }
                                    .forEach {
                                        cookieMap[it[0]] = it[1] }
                        } else {
                            headerMap[it[0]] = it[1]
                        }
                    }
                }

        val splm = firstLine.split(" ")
        this.resourcePath = splm[1]
        this.method = Method.valueOf(splm[0])
    }

    private fun processCookies(): String {
        if (cookieMap.isEmpty()){
            return "count=1"
        } else {
            cookieMap.map { it }
                    .forEach {
                        return if (it.key == "count") {
                            var value = Integer.parseInt(it.value)
                            value++
                            return "count=$value"
                        } else {
                            return "count=1"
                        }
                    }
        }
        return "count=1"
    }

    private fun errorMessage(connection: Socket, errorType: String, message: String, title: String,
                             writer: PrintStream, cookieCount: String) {
        writer.print("HTTP/1.1 $errorType\r\n")
        writer.print("Set-Cookie: $cookieCount;\r\n\r\n")

        writer.print("<!DOCTYPE html>\r\n" +
                "<html>\r\n<head>\r\n" +
                "<title>$title</title>\r\n</head>\r\n" +
                "<body>\r\n" +
                "<h1>$message</h1>\r\n" +
                "<hr><address>FileServer at " +
                connection.localAddress.hostName +
                " Port " + connection.localPort + "</address><hr>\r\n" +
                "</body>\r\n</html>\r\n")
        writer.flush()

    }

    private fun dispayFilesTable(connection: Socket, file: File, cookieCount: String) {
        val writer = BufferedWriter(OutputStreamWriter(connection.getOutputStream()))

        writer.write("HTTP/1.1 200 \r\n")
        writer.write("Set-Cookie: $cookieCount;\r\n\r\n")

        //val headTable = File("..\\frontEnd\\tableHead.html")
        //writer.write(headTable.readText())
        writer.write("<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <style>\n" +
                "table, th, td {\n" +
                "    border: 1px solid black;\n" +
                "    border-collapse: collapse;\n" +
                "}\n" +
                "th, td {\n" +
                "    padding: 5px;\n" +
                "    text-align: left;\n" +
                "}\n" +
                "</style>\n" +
                "</head>")
        writer.write("<h2>Arquivos de " + file.name + "</h2>")
        writer.write("<table style=\"width:100%\">" +
                "<caption>Arquivos</caption>\n")
        writer.write("<tr>\n" +
                "<th>Nome</th>\n" +
                "<th>Modificado</th>\n" +
                "<th>Tamanho</th>\n" +
                "</tr>\n" +
                "</html>")

        val fileList = file.listFiles()

        for (item in fileList) {
            writer.write("<tr>\n" +
                    "<td><a href=\"http://www.localhost:5555/" + item.absolutePath.substringAfter("C:\\").replace("\\", "/")
                    + "\";>" + item.name + "</a></td>\n" +
                    "<td>" + SimpleDateFormat("dd/MM/yyyy").format(item.lastModified()) + "</td>\n" +
                    "<td>" + item.length() + " bytes</td>\n" +
                    "</tr>\n")
        }
        writer.write("</table>\n" +
                "</body>\n" +
                "</html>")

        writer.flush()
        connection.close()
    }

    private fun authenticate(connection: Socket):Boolean {
        return headerMap["Authorization"] == "Basic MTIzOjEyMw=="
    }

    private fun responseGet(writer: PrintStream/*, reader: BufferedReader*/, connection: Socket) {
        val file = File("C:"+this.resourcePath)

        //file = File("C:/Users/Lucas/Desktop/LeBoidAvidyaizumi/0UTFPR/8/web/Socket/src/test.html")
        //print(file.listFiles().size)

        val count = processCookies()

        if (!file.exists() /*|| this.resourcePath != "/"*/) {
            errorMessage(connection, "404", "Arquivo não encontrado!", "Error 404", writer, count)
        } else if (file.isDirectory) {
            writer.print("HTTP/1.1 401\r\n")
            writer.print("WWW-Authenticate: Basic realm=Aut\r\n\r\n")
            writer.flush()

            val connection = this.mySocket.accept()
            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))

            this.processHeader(reader)

            if(this.resourcePath == "/") {
                this.resourcePath = "C:\\Users"
            }

            if(authenticate(connection)) {
                dispayFilesTable(connection, file, count)
            }
        } else if(file.name.substringAfterLast('.') == "exe") {
            executavel(file.path, null, writer)
        } else {
            writer.print("HTTP/1.1 200 \r\n")
            writer.print("Set-Cookie: $count;\r\n")

            val contentType = when (file.name.substringAfterLast('.')) {
                "jpg" -> "image/jpeg"
                "pgn" -> "image/png"
                "pdf" -> "application/pdf"
                "mp4" -> "video/mp4"
                else -> "application/octet-stream"
            }

            writer.print("Content-Type: $contentType\r\n")

            val fileBytes = file.readBytes()

            writer.print("Content-Length: ${fileBytes.size}\r\n\r\n")
            writer.flush()

            connection.getOutputStream().write(fileBytes)
            connection.getOutputStream().flush()
        }
    }

    fun run() {
        print("Server on \n\n")

        while(true) {
            val connection = this.mySocket.accept()

            if (connection != null) {
                Thread({
                    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
                    //val writer = BufferedWriter(OutputStreamWriter(connection.getOutputStream()))
                    val out = BufferedOutputStream(connection.getOutputStream())
                    val writer = PrintStream(out)

                    this.processHeader(reader)

                    println("\nBEFORE:")
                    println(headerMap)

                    when (this.method) {
                        Method.GET -> this.responseGet(writer/*, reader*/, connection)
                    }
                    println("\nAFTER:")
                    print(headerMap)

                    println("\nCOOKIES:")
                    print(cookieMap)

                    connection.close()
                    reader.close()
                    writer.close()
                }).run()
            }
        }
    }
}

fun main(argv: Array<String>) {
    val server = Server()

    server.run()
    //server.executavel("C:\\Users\\Convidado\\Desktop\\exec\\textexec.exe")
}