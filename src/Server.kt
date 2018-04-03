import java.io.*
import java.net.*


class Server {
    enum class Method {
        OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT
    }

    private val mySocket = ServerSocket(5555)
    private lateinit var headerMap: MutableMap<String, String>
    private lateinit var resourcePath: String
    private lateinit var method: Method
    private lateinit var cookieMap: MutableMap<String, String>

    private fun processHeader(reader: BufferedReader) {
        val firstLine = reader.readLine()
        var header = reader.readLine()

        while (reader.ready()) {
            header += "\n"
            header += reader.readLine()
        }
        print(header)

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

    private fun fileList(directory: File): List<String> {
        val fileList = mutableListOf<String>()

        var filePath = directory.toString()
        var init = filePath.lastIndexOf("\\")
        var fileName =  filePath.substring(init + 1)
        fileList.add(fileName)

        for (file in directory.listFiles()) {
            filePath = file.toString()
            init = filePath.lastIndexOf("\\")
            //print("\n\nCAMINHO DO NARA\n\n")
            //print(filePath + "\n\n")
            fileName = filePath.substring(init + 1)
            fileList.add(fileName)
        }
        return fileList
    }

    private fun responseGet(writer: BufferedWriter) {
        val file = File("C:"+this.resourcePath)
        //file = File("C:/Users/Lucas/Desktop/LeBoidAvidyaizumi/0UTFPR/8/web/Socket/src/test.html")
        //print(file.listFiles().size)
        val count = processCookies()
        writer.write("HTTP/1.1 401\r\n")
        writer.write("WWW-Authenticate: Basic realm=Test\r\n")
        if (!file.exists() || this.resourcePath == "/") {
            writer.write("HTTP/1.1 404\r\n")
            writer.write("Set-Cookie: $count;\r\n\r\n")

            writer.write("ERRO: Arquivo nao encotrado")
        } else if (file.isDirectory) {
            //file.listFiles()
            writer.write("HTTP/1.1 200 \r\n")
            writer.write("Set-Cookie: $count;\r\n\r\n")

            val fileList = fileList(file)
            writer.write("Arquivos de " + fileList[0] + ":\n")
            for (i in 1..(fileList.size - 1)) {
                writer.write("/" + fileList[i] + "\n")
            }
        } else {
            writer.write("HTTP/1.1 200 \r\n")
            writer.write("Set-Cookie: $count;\r\n\r\n")

            val content = FileReader(file)
            val bffct = BufferedReader(content)
            var line: String
            while (bffct.ready()) {
                line = bffct.readLine()
                writer.write(line)
            }
        }
        writer.flush()
    }

    fun run() {
        print("Server on \n\n")

        while(true) {
            val connection = this.mySocket.accept()
            if (connection != null) {
                Thread({
                    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
                    val writer = BufferedWriter(OutputStreamWriter(connection.getOutputStream()))

                    this.processHeader(reader)

                    when (this.method) {
                        Method.GET -> this.responseGet(writer)
                    }

                    print(headerMap)
                    println("\nCOOKIES: ")
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
}