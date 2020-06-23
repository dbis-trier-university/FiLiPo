package Similarity.Classifier;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class GbClassifier {

    private ZContext context;
    private ZMQ.Socket socket;

    public GbClassifier(String url){
        context = new ZContext();
        this.socket = context.createSocket(SocketType.REQ);
        this.socket.connect(url);
    }

    public boolean isEquals(String id1, String id2){
        String request = id1 + " -;- " + id2;
        this.socket.send(request.getBytes(ZMQ.CHARSET), 0);

        byte[] reply = this.socket.recv(0);
        String replyString = new String(reply, ZMQ.CHARSET);

        return Integer.parseInt(replyString) == 1;
    }

    public void close(){
        this.context.close();
        this.socket.close();
    }
}
