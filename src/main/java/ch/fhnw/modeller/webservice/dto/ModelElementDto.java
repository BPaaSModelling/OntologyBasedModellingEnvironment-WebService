package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

@Data
public class ModelElementDto {

    private String id;
    private Map<String, String> attributes;

    public ModelElementDto(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelElementDto that = (ModelElementDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
