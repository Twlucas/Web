import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.DatagramSocket

class EchoServer(val server: Server?) : Thread() {
    private val broadcastPort = 5554
    private val responsePort = 5556
    private val httpPort = 5555
    private val address = InetAddress.getByName("255.255.255.255")
    private lateinit var serverList: MutableMap<InetAddress, Int>

    private fun packetData(data: String, type: String): Int{
        return when {
            data.indexOf("AD") > -1 && type == "AD" -> {
                val port = data.substring(2).replace("\n","").toInt()
                port
            }
            data.indexOf("SD") > -1 && type == "SD" -> {
                val port = data.split(' ')[1].replace("\n","").toInt()
                port
            }
            data.indexOf("SD") > -1 && type == "AD" -> {
                val port = data.substring(2).split(" ")[0].replace("\n","").toInt()
                port
            }
            else -> -1
        }
    }

    private fun serverDiscovery() {
        println("Discovery")
        val broadcastMessage = "SD$responsePort $httpPort"
        Broadcasting.broadcast(broadcastMessage, address)

        val socket = DatagramSocket(responsePort)
        val buf = ByteArray(256)

        val packet = DatagramPacket(buf, buf.size)

        Thread( {
            while (true) {
                socket.receive(packet)

                val address = packet.address
                val received = String(packet.data, 0, packet.length)

                val dPort = packetData(received, "AD")
                if(dPort != -1) {
                    if(serverList[address] == null) {
                        serverList[address] = dPort
                        this.server!!.setServerList(serverList)
                    }
                }
                /*

                packet = DatagramPacket(buf, buf.size, address, port)

                println(received)

                if (received == "end") {
                    //running = false
                }

                socket.send(packet)*/
            }
        }).start()
    }

    private fun portListener() {
        println("Listener")
        val socket = DatagramSocket(broadcastPort)
        val buf = ByteArray(256)
        val packet = DatagramPacket(buf, buf.size)

        Thread( {
            while (true) {
                socket.receive(packet)

                val address = packet.address
                val received = String(packet.data, 0, packet.length)

                var dPort = packetData(received, "SD")
                if(dPort != -1) {
                    println("entroSD")
                    if(serverList[address] == null) {
                        serverList[address] = dPort
                        this.server!!.setServerList(serverList)
                    }
                }

                dPort = packetData(received, "AD")

                val msg = "AD8080\n".toByteArray()
                val sendPacket = DatagramPacket(msg, msg.size, address, dPort)

                val sendSocket = DatagramSocket()
                sendSocket.send(sendPacket)
            }
        }).start()
    }

    override fun run() {
        println("ECHO SERVER ON")
        this.serverList = mutableMapOf()
        serverDiscovery()
        portListener()
    }

    fun getServerList(): MutableMap<InetAddress, Int>? {
        return if(this.serverList.isNotEmpty())
            this.serverList
        else
            null
    }

}

fun main(argv: Array<String>) {
    val server = EchoServer(null)
    //server.run()
    //Broadcasting.broadcast("AD8080\n")
    sleep(2000)
    println("Sleep")
    val dPort = 5554
    val address = InetAddress.getByName("255.255.255.255")

    val msg = "SD5556 8080\n".toByteArray()
    val sendPacket = DatagramPacket(msg, msg.size, address, dPort)

    val sendSocket = DatagramSocket()
    sendSocket.send(sendPacket)
    sleep(1000)
    println("Lst" + server.getServerList())
}