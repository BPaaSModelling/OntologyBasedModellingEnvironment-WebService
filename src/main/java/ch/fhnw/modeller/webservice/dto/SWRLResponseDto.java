package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class SWRLResponseDto {
    private List<String> results;
    private String error;

    public SWRLResponseDto() {
    }

}


