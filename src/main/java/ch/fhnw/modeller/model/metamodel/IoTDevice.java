package ch.fhnw.modeller.model.metamodel;

public class IoTDevice {
    private String xCOR;
    private String yCOR;
    private String zCOR;
    private boolean sucCup;
    private boolean cal;
    private String modelId;
    private String name;

    // Default constructor
    public IoTDevice() {
    }

    // Getters and Setters
    public String getxCOR() {
        return xCOR;
    }

    public void setxCOR(String xCOR) {
        this.xCOR = xCOR;
    }

    public String getyCOR() {
        return yCOR;
    }

    public void setyCOR(String yCOR) {
        this.yCOR = yCOR;
    }

    public String getzCOR() {
        return zCOR;
    }

    public void setzCOR(String zCOR) {
        this.zCOR = zCOR;
    }

    public boolean isSucCup() {
        return sucCup;
    }

    public void setSucCup(boolean sucCup) {
        this.sucCup = sucCup;
    }

    public boolean isCal() {
        return cal;
    }

    public void setCal(boolean cal) {
        this.cal = cal;
    }

    public String getModelid() {
        return modelId;
    }

    public void setModelid(String modelId) {
        this.modelId = modelId;
    }

    public String getNameIoT() {
        return name;
    }

    public void setNameIoT(String name) {
        this.name = name;
    }
}
