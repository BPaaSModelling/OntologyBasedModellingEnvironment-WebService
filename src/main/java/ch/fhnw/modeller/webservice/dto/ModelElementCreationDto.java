package ch.fhnw.modeller.webservice.dto;

import lombok.Data;

@Data
public class ModelElementCreationDto {

    private String paletteConstruct;
    private int x;
    private int y;
    private int w;
    private int h;
    private String uuid;
    private String label;
    private String modelingLanguageConstructInstance;
    private InstantiationTargetType instantiationType;
    private String note;
    private String shapeRepresentsModel;
    private String xCoorDobotMagician;
    private String yCoorDobotMagician;
    private String zCoorDobotMagician;
    private boolean sucCupOnDobotMagician;
    private boolean sucCupOffDobotMagician;
    private boolean calDobotMagician;









    public String getxCoorDobotMagician() {
        return xCoorDobotMagician;
    }

    public void setxCoorDobotMagician(String xCoorDobotMagician) {
        this.xCoorDobotMagician = xCoorDobotMagician;
    }

    public String getyCoorDobotMagician() {
        return yCoorDobotMagician;
    }

    public void setyCoorDobotMagician(String yCoorDobotMagician) {
        this.yCoorDobotMagician = yCoorDobotMagician;
    }

    public String getzCoorDobotMagician() {
        return zCoorDobotMagician;
    }

    public void setzCoorDobotMagician(String zCoorDobotMagician) {
        this.zCoorDobotMagician = zCoorDobotMagician;
    }

    public boolean getSucCupOnDobotMagician() {
        return sucCupOnDobotMagician;
    }

    public void setSucCupOnDobotMagician(boolean sucCupOnDobotMagician) {
        this.sucCupOnDobotMagician = sucCupOnDobotMagician;
    }

    public boolean getSucCupOffDobotMagician() {
        return sucCupOffDobotMagician;
    }

    public void setSucCupOffDobotMagician(boolean sucCupOffDobotMagician) {
        this.sucCupOffDobotMagician = sucCupOffDobotMagician;
    }

    public boolean getCalDobotMagician() {
        return calDobotMagician;
    }

    public void setCalDobotMagician(boolean calDobotMagician) {
        this.calDobotMagician = calDobotMagician;
    }
}


