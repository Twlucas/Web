import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Objects
import java.net.NetworkInterface
import java.util.ArrayList
import java.net.SocketException


object Broadcasting {
    @Throws(IOException::class)
    fun broadcast(
            broadcastMessage: String,
            address: InetAddress = InetAddress.getByName("255.255.255.255")) {
        val socket = DatagramSocket(5554)
        //val socket = DatagramSocket()
        //val socket = DatagramSocket()
        //socket.connect(address, 5554)
        socket.broadcast = true

        val buffer = broadcastMessage.toByteArray()

        val packet = DatagramPacket(buffer, buffer.size, address, 5554)
        socket.send(packet)
        socket.close()
    }

    @Throws(SocketException::class)
    fun listAllBroadcastAddresses(): List<InetAddress> {
        val broadcastList = ArrayList<InetAddress>()
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()

            if (networkInterface.isLoopback || !networkInterface.isUp) {
                continue
            }

            networkInterface.interfaceAddresses.stream()
                    .map { a -> a.broadcast }
                    .filter({ Objects.nonNull(it) })
                    .forEach({ broadcastList.add(it) })
        }
        return broadcastList
    }
}