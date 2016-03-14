package br.com.model.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by MarioJ on 26/03/15.
 */
public class Person implements Serializable {

    public static final String ID = "id";
    public static final String DDI = "ddi";
    public static final String PHONE = "phone";
    public static final String NAME = "name";
    public static final String STATUS = "status";
    public static final String LATITUDE = "lat";
    public static final String LONGITUDE = "lon";
    public static final String PHOTO_URI = "photo_uri";
    public static final String PHOTO_URI_THUMB = "photo_uri_tn";

    private Integer id;
    private String ddi;
    private String phone;
    private String name;
    private String status;
    private Date date;
    private Double latitude;
    private Double longitude;
    private byte[] photo;

    public Person() {
    }

    public Person(Integer id, String name, String ddi, String phone, byte[] photo, String status) {
        this.id = id;
        this.name = name;
        this.ddi = ddi;
        this.phone = phone;
        this.photo = photo;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDdi() {
        return ddi;
    }

    public void setDdi(String ddi) {
        this.ddi = ddi;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        return !(id != null ? !id.equals(person.id) : person.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", ddi='" + ddi + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
