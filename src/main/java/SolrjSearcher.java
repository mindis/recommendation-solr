/**
 * Created by root on 3/19/15.
 */
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import java.net.MalformedURLException;

public class SolrjSearcher {
    public static void main(String[] args) throws MalformedURLException, SolrServerException {
        String QUERY="web"; //query to search
        HttpSolrServer solr = new HttpSolrServer("http://localhost:8983/solr/arduino_core/");
        SolrQuery query = new SolrQuery();
        query.set("defType", "dismax"); //setting dismax query parser
        query.set("q.alt", "(Title:*"+QUERY+"*)^10  (Tags:*"+QUERY+"*)"); //setting parameter to search on title and Tags and give more boost to title wise results
        query.set("bf","linear(Score,2,1) linear(AnswerCount,1.5,1)"); //boost doc rank  by score*2 and AnswerCount by 1.5
        //query.set("qf", "Title^2 Tags"); //setting querying fields

        query.setFields("Title", "AnswerCount", "Score", "ViewCount");
        //System.out.println(query.toString());
        QueryResponse response = solr.query(query);

        SolrDocumentList results = response.getResults();

        for (int i = 0; i < results.size(); ++i) {
            System.out.println(results.get(i));
        }
    }
}
