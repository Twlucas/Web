import java.io.*
import java.net.*



class Client(){
    fun clientRun() {
        try {
            val socketClient = Socket("localhost", 5555)
            println("Client: " + "Connection Established")

            val reader = BufferedReader(InputStreamReader(socketClient.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socketClient.getOutputStream()))

            writer.write("+\r\n")
            println("enviado 8")
            writer.write("8\r\n")
            println("enviado 8")
            writer.write("10\r\n")
            println("enviado 10")
            writer.flush()

            var serverMsg = reader.readLine()

            while (serverMsg != null) {
                println("Client: " + serverMsg)
                serverMsg = reader.readLine()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}

fun main(argv: Array<String>) {
    val client = Client()

    client.clientRun()
}