import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;


public class MyScanner {
    private final BufferedReader br;
    private StringTokenizer st;

    MyScanner(FileReader br) {
        this.br = new BufferedReader(br);
    }

    public String next() throws IOException {
        while (st == null || !st.hasMoreTokens()) {
            st = new StringTokenizer(br.readLine());
        }
        return st.nextToken();
    }

    public int nextInt() throws IOException {
        return Integer.parseInt(next());
    }

    public String nextLine() throws IOException{
        String str = "";
        str = br.readLine();
        st = null;
        return str;
    }
}

