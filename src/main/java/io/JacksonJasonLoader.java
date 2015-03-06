package io;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.MappingJsonFactory;
import util.MurmurHash;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by vishnu on 6/3/15.
 */
public class JacksonJasonLoader {
    static HttpSolrServer HOTEL_CONNECTOR = new HttpSolrServer("http://localhost:8983/solr/recHotel");
    static HttpSolrServer REVIEW_CONNECTOR = new HttpSolrServer("http://localhost:8983/solr/recReview");
    static Pattern numberPattern = Pattern.compile("[0-9]+");
    public static void readFromFile(String filePath) {
        try {
            JsonFactory f = new MappingJsonFactory();
            File jsonFile = new File(filePath);
            Long hotelId = Long.parseLong(jsonFile.getName().replaceFirst("[.][^.]+$", ""));
            JsonParser jp = f.createJsonParser(jsonFile);
            JsonToken current;
            current = jp.nextToken();
            if (current != JsonToken.START_OBJECT) {
                System.out.println("Error: root should be object: quiting.");
                return;
            }

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jp.getCurrentName();
                current = jp.nextToken();
                if (fieldName.equals("Reviews")) {
                    if (current == JsonToken.START_ARRAY) {
                        while (jp.nextToken() != JsonToken.END_ARRAY) {
                            JsonNode review = jp.readValueAsTree();
                            SolrInputDocument reviewDoc = new SolrInputDocument();
                            JsonNode ratings = review.get("Ratings");
                            Iterator<String> ratingIterator = ratings.getFieldNames();

                            reviewDoc.addField("id", MurmurHash.hash64(review.get("ReviewID").asText()));
                            reviewDoc.addField("HotelId", hotelId);
                            reviewDoc.addField("AuthorName",review.get("Author").asText());
                            reviewDoc.addField("AuthorLocation",review.get("AuthorLocation").asText());
                            reviewDoc.addField("ReviewId",review.get("ReviewID").asText());
                            reviewDoc.addField("ReviewContent",review.get("Content").asText());
                            reviewDoc.addField("ReviewTitle",review.get("Title").asText());

                            while (ratingIterator.hasNext()) {
                                String ratingName = ratingIterator.next();
                                reviewDoc.addField(ratingName,Double.parseDouble(ratings.get(ratingName).asText()));
                            }

                            REVIEW_CONNECTOR.add(reviewDoc);
                        }
                        REVIEW_CONNECTOR.commit();
                    } else {
                        System.out.println("Error: records should be an array: skipping.");
                        jp.skipChildren();
                    }
                } else if (fieldName.equals("HotelInfo")) {
                    JsonNode hotelInfo = jp.readValueAsTree();
                    SolrInputDocument hotelDoc = new SolrInputDocument();
                    hotelDoc.addField("HotelName",hotelInfo.get("Name").asText());
                    hotelDoc.addField("HotelId",hotelId);
                    hotelDoc.addField("id",hotelId);
                    hotelDoc.addField("HotelUrl",hotelInfo.get("HotelURL").asText());
                    hotelDoc.addField("HotelAddress",hotelInfo.get("Address").asText());
                    hotelDoc.addField("HotelImgUrl",hotelInfo.get("ImgURL").asText());
                    
                    String price = hotelInfo.get("Price").asText();
                    String[] pricelist = price.split("-");

                    if(pricelist.length==2){
                        pricelist[0] = pricelist[0].replaceAll("[^-?0-9]+", "");
                        pricelist[1] = pricelist[1].replaceAll("[^-?0-9]+", "");
                        long minPrice=Long.parseLong(pricelist[0]);
                        long maxPrice=Long.parseLong(pricelist[1]);
                        System.out.println("Price fot hotel "+hotelId+" is "+price+" min:"+minPrice+" max:"+maxPrice);
                        hotelDoc.addField("HotelMinPrice",minPrice);
                        hotelDoc.addField("HotelMaxPrice",maxPrice);
                    }
                    
                    hotelDoc.addField("Price",price);
                    HOTEL_CONNECTOR.add(hotelDoc);
                    HOTEL_CONNECTOR.commit();
                    
                }else{
                    System.out.println("Unprocessed property: " + fieldName);
                    jp.skipChildren();
                }
            }


        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SolrServerException e) {
            e.printStackTrace();
        }

    }
}