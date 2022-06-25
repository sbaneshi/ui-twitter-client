package ir.ac.ui.uitwitterclient.api;

import lombok.Data;

import java.util.List;

@Data
public class GetHistoryResponse {

    List<Tweet> tweets;
}
