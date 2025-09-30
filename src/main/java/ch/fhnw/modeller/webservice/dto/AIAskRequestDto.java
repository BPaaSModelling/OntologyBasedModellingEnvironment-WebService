package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class AIAskRequestDto {

    String userMessage;
    String swrlRule;
    String ttlContent;

    public AIAskRequestDto() {
    }

}
