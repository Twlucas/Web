import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.PrintStream
import java.io.BufferedOutputStream
import java.util.Collections.sort
import java.lang.Thread.sleep

class Server {
    enum class Method {
        OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT
    }

    private val httpPort = 5555
    //private val httpPort = 5555
    private lateinit var mySocket: ServerSocket
    private lateinit var headerMap: MutableMap<String, String>
    private lateinit var resourcePath: String
    private lateinit var method: Method
    private lateinit var cookieMap: MutableMap<String, String>
    private lateinit var paramList: MutableMap<String, String>
    private var serverList: MutableMap<InetAddress, Int>? = null
    private var fromServer = false
    private lateinit var firstLine: String

    fun setServerList(serverList: MutableMap<InetAddress, Int>?){
        println("SERVERLIST")
        this.serverList = serverList
        println(this.serverList)
    }

    private fun cgi(path: String, params: String? = null, writer: PrintStream) {
        val processBuilder = ProcessBuilder()

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
        this.firstLine = reader.readLine()
        var header = reader.readLine()

        while (reader.ready()) {
            header += "\n"
            header += reader.readLine()
        }

        println("RAWHEADER")
        print(firstLine)
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
                            if(it[0] == "SERVER-TO-SERVER"){
                                fromServer = true
                            }
                            headerMap[it[0]] = it[1]
                        }
                    }
                }

        val splm = firstLine.split(" ")
        this.resourcePath = splm[1]

        this.resourcePath = this.resourcePath.replace("%20", " ")

        this.paramList = mutableMapOf()

        if(this.resourcePath.indexOf('?') > -1) {
            val qparams = this.resourcePath.split('?')

            this.resourcePath = qparams[0]

            this.getParams(qparams[1])
        }

        this.method = Method.valueOf(splm[0])
    }

    private fun getParams(params: String) {
        if(params.indexOf('&') > -1) {
            params.split('&')
                    .forEach{
                        val splt = it.split('=')
                        if (splt.isNotEmpty()){
                            this.paramList[splt[0]] = splt[1]
                        }
                    }
        } else {
            val splt = params.split('=')
            if (splt.isNotEmpty()){
                this.paramList[splt[0]] = splt[1]
            }
        }
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
                            "count=$value"
                        } else {
                            "count=1"
                        }
                    }
        }
        return "count=1"
    }

    private fun errorMessage(connection: Socket, errorType: String, message: String, title: String,
                             writer: PrintStream, cookieCount: String) {
        if(errorType == "404") {
            writer.print("HTTP/1.1 $errorType\r\n")
            writer.print("Set-Cookie: $cookieCount;\r\n\r\n")

            writer.print(File("./view/form404.html").readText())
            writer.flush()
        } else {
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

    }

    private fun displayFilesTable(connection: Socket, file: File, cookieCount: String) {
        val writer = BufferedWriter(OutputStreamWriter(connection.getOutputStream()))

        writer.write("HTTP/1.1 200 \r\n")
        writer.write("Set-Cookie: $cookieCount;\r\n\r\n")

        val headTable = File("./view/tableHead.html")
        writer.write(headTable.readText())

        var fileName = file.name

        if(fileName == "") {
            fileName = "Home"
        }

        writer.write("\n<h2>Arquivos de " + fileName + "</h2>\n")

        writer.write("<div class=\"container\">\n")

        writer.write("\n<table class=\"table table-sm table-bordered table-hover\" style=\"width:100%\">" +
                "<caption>Arquivos</caption>\n")

        val host = headerMap["Host"]
        writer.write("<thead class=\"thead-light\">\n")
        writer.write("<tr>\n" +
                "<th><a href=\"http://$host$resourcePath" + "?O=N\";>" + "Nome</a></th>\n" +
                "<th><a href=\"http://$host$resourcePath" + "?O=M\";>" + "Modificado</a></th>\n" +
                "<th><a href=\"http://$host$resourcePath" + "?O=T\";>" + "Tamanho</a></th>\n" +
                "</tr>\n")
        writer.write("</thead>\n")

        val fileList = file.listFiles().asList()
        //fileList.sort()

        if(paramList.isNotEmpty()) {
            val type = paramList["O"]
            sort(fileList, FileSorter(paramList["O"]))
        } else {
            sort(fileList, FileSorter())
        }

        if(this.resourcePath[resourcePath.length-1] == '/') {
            resourcePath = resourcePath.substring(0, resourcePath.length-1)
        }

        for (item in fileList) {
            println("\n\n\nFILEPATH:::::: http://$host/${item.path.replace("\\", "/")}" + "\n" + item.name + "\n\n\n")
            writer.write("<tr>\n" +
                    "<td><a href=\"http://$host/${item.path.replace("\\", "/")}\";>" + item.name + "</a></td>\n" +
                    //"<td><a href=\"http://$host$resourcePath/" + item.name + "\";>" + item.name + "</a></td>\n" +
                    "<td>" + SimpleDateFormat("dd/MM/yyyy").format(item.lastModified()) + "</td>\n" +
                    "<td>" + item.length() + " bytes</td>\n" +
                    "</tr>\n")
        }

        writer.write("</table>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>")

        writer.flush()
        connection.close()
    }

    private fun authenticate():Boolean {
        return if(headerMap["Authorization"] != null) {
            headerMap["Authorization"] == "Basic MTIzOjEyMw=="
        } else
            false
    }

    private fun hasError(line: String): Boolean {
        return line.indexOf("HTTP/1.1 404") > -1
    }

    private fun responseGet(writer: PrintStream/*, reader: BufferedReader*/, connection: Socket) {
        if(this.resourcePath == "/index.html") {
            this.resourcePath = ""
        }

        var file = File(this.resourcePath)

        if (!file.exists()) {
            file = File("C:" + this.resourcePath)
        }

        val count = processCookies()

        if (!file.exists()) {
            var hasSend = false
            if(this.serverList != null && !this.fromServer){
                var rawHeader: String = "$firstLine\r\n"
                println("MAPERRO")

                this.headerMap.forEach {
                    rawHeader += it.key + ": " + it.value + "\r\n"
                }

                rawHeader += "Cookie: "
                this.cookieMap.forEach {
                    rawHeader += it.key + "=" + it.value + ";"
                }

                rawHeader = rawHeader.substring(0, rawHeader.lastIndex)
                rawHeader += "\r\n"

                rawHeader += "SERVER-TO-SERVER: true\r\n\r\n"

                println("ServerLIst: $serverList")

                serverList!!.forEach {
                    val newServerSocket = Socket(it.key, it.value)
                    newServerSocket.getOutputStream().write(rawHeader.toByteArray())
                    val newServerReader = BufferedReader(InputStreamReader(newServerSocket.getInputStream()))
                    val fline = newServerReader.readLine()
                    if(hasError(fline)) {
                        hasSend = false
                        println("FALSEANO")
                    } else {
                        writer.print(fline)
                        writer.print(newServerReader.readText())
                        writer.flush()
                        hasSend = true
                        return@forEach
                    }
                }
            }
            if(!hasSend) {
                errorMessage(connection, "404", "Arquivo não encontrado!", "Error 404", writer, count)
            }
        } else if (file.isDirectory) {
            if(authenticate() || !this.cookieMap["count"].isNullOrEmpty()) {
                displayFilesTable(connection, file, count)
            } else {
                var strResponse = "HTTP/1.1 401\r\n"

                writer.print("HTTP/1.1 401\r\n")
                writer.print("WWW-Authenticate: Basic\r\n\r\n")

                writer.flush()

                strResponse += "WWW-Authenticate: Basic\"\r\n\r\n"

                println("SEND1")
                println(strResponse)
            }
        } else if(file.name.substringAfterLast('.') == "exe") {
            cgi(file.path, null, writer)
        } else {
            writer.print("HTTP/1.1 200\r\n")
            var strResponse = "HTTP/1.1 200\r\n"
            writer.print("Set-Cookie: $count;\r\n")
            strResponse += "Set-Cookie: $count;\r\n"

            val contentType = when (file.name.substringAfterLast('.')) {
                "jpg" -> "image/jpeg"
                "pgn" -> "image/png"
                "pdf" -> "application/pdf"
                "mp4" -> "video/mp4"
                else -> "application/octet-stream"
            }

            writer.print("Content-Type: $contentType\r\n")

            strResponse += "Content-Type: $contentType\r\n"

            val fileBytes = file.readBytes()

            writer.print("Content-Length: ${fileBytes.size}\r\n\r\n")
            writer.flush()

            strResponse += "Content-Length: ${fileBytes.size}\r\n\r\n"

            connection.getOutputStream().write(fileBytes)
            connection.getOutputStream().flush()
            strResponse += fileBytes

            println("STRRESPONSE")
            println(strResponse)
        }
        println("SEND")
    }

    private fun responsePost(writer: PrintStream, connection: Socket) {
        val count = processCookies()

        writer.print("HTTP/1.1 200\r\n")
        writer.print("Set-Cookie: $count;\r\n\r\n")

        writer.print(File("./view/form404.html").readText())
        writer.flush()

    }

    fun run() {
        mySocket = ServerSocket(httpPort)
        print("Server on \n\n")

        while(true) {
            val connection = this.mySocket.accept()
            println("Accepted")

            if (connection != null) {
                Thread {
                    this.fromServer = false

                    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))

                    //val reader = BufferedInputStream(connection.getInputStream())

                    //val writer = BufferedWriter(OutputStreamWriter(connection.getOutputStream()))

                    val out = BufferedOutputStream(connection.getOutputStream())

                    val writer = PrintStream(out)
                    println("PROCESS")

                    this.processHeader(reader)

                    println("\nMAP")
                    println(this.resourcePath)
                    println("\n" + this.headerMap)

                    println("COOKIE")
                    println("\n" + this.cookieMap)

                    when (this.method) {
                        Method.GET -> this.responseGet(writer/*, reader*/, connection)
                        Method.OPTIONS -> TODO()
                        Method.HEAD -> TODO()
                        Method.POST -> this.responsePost(writer, connection)
                        Method.PUT -> TODO()
                        Method.DELETE -> TODO()
                        Method.TRACE -> TODO()
                        Method.CONNECT -> TODO()
                    }

                    //print("\n" + headerMap)

                    //println("\nCOOKIES:")
                    //print(cookieMap)

                    //println("\nPARAMS\n")
                    //print(paramList)

                    connection.close()
                    reader.close()
                    writer.close()
                }.start()//.run()
            }
        }
    }
}

fun main(argv: Array<String>) {
    val server = Server()

    val echoServer = EchoServer(server)

    echoServer.run()

    server.setServerList(echoServer.getServerList())
    server.run()

    /*val file = File("C:/Users/Convidado/Desktop/exec")
    val files = file.listFiles()
    val mu = files.asList()
    val fl = FileSorter()
    sort(mu, fl)
    mu.forEach({
        println(it.nameWithoutExtension)
    })*/
    //EchoServer().run()
    //Broadcasting.broadcast("LUL")
    /*Thread.sleep(2000)
    println("Sleep")
    val dPort = 5554
    val address = InetAddress.getByName("255.255.255.255")

    val msg = "SD5556 8080\n".toByteArray()
    val sendPacket = DatagramPacket(msg, msg.size, address, dPort)

    val sendSocket = DatagramSocket()
    sendSocket.send(sendPacket)
    Thread.sleep(1000)
    println("Lst" + echoServer.getServerList())*/
}
