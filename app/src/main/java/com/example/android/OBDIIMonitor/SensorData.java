package com.example.android.OBDIIMonitor;

/**
 * Created by rogy on 2/1/15.
 */
public class SensorData {
    private long timestamp;
    private String IntakeTemperature;
    private String EngineLoad;
    private String CoolantTemperature;
    private String RPM;
    private String Mph;
    private String Voltage;


    public SensorData(long timestamp, String mIntakeTemperature,
                      String mEngineLoad,
                      String mCoolantTemperature,
                      String mRPM,
                      String mMph,
                      String mVoltage) {
        this.timestamp = timestamp;
        this.IntakeTemperature = mIntakeTemperature;
        this.EngineLoad = mEngineLoad;
        this.CoolantTemperature = mCoolantTemperature;
        this.RPM = mRPM;
        this.Mph = mMph;
        this.Voltage = mVoltage;

    }




    public String getIntakeTemperature() {
        return IntakeTemperature;
    }

    public void setIntakeTemperature(String intakeTemperature) {
        IntakeTemperature = intakeTemperature;
    }

    public String getEngineLoad() {
        return EngineLoad;
    }

    public void setEngineLoad(String engineLoad) {
        EngineLoad = engineLoad;
    }

    public String getCoolantTemperature() {
        return CoolantTemperature;
    }

    public void setCoolantTemperature(String coolantTemperature) {
        CoolantTemperature = coolantTemperature;
    }

    public String getMph() {
        return Mph;
    }

    public void setMph(String mph) {
        Mph = mph;
    }

    public String getRPM() {
        return RPM;
    }

    public void setRPM(String RPM) {
        this.RPM = RPM;
    }

    public String getVoltage() {
        return Voltage;
    }

    public void setVoltage(String voltage) {
        Voltage = voltage;
    }





    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public String toString() {
        return "SensorData{" +
                "timestamp=" + timestamp +
                ", IntakeTemperature='" + IntakeTemperature + '\'' +
                ", EngineLoad='" + EngineLoad + '\'' +
                ", CoolantTemperature='" + CoolantTemperature + '\'' +
                ", RPM='" + RPM + '\'' +
                ", Mph='" + Mph + '\'' +
                ", Voltage='" + Voltage + '\'' +
                '}';
    }

}

