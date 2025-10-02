package com.codelab.micproject.account.consultant.dto;

import org.springframework.web.multipart.MultipartFile;

public class UpdateConsultantForm {
    private String name;
    private String phone;
    private String email;
    private String company;
    private String specialty;
    private String introduction;
    private Integer careerStartYear;
    private MultipartFile profileImage; // form-data의 "profileImage"

    // getters / setters
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone = phone; }
    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }
    public String getCompany(){ return company; }
    public void setCompany(String company){ this.company = company; }
    public String getSpecialty(){ return specialty; }
    public void setSpecialty(String specialty){ this.specialty = specialty; }
    public String getIntroduction(){ return introduction; }
    public void setIntroduction(String introduction){ this.introduction = introduction; }
    public Integer getCareerStartYear(){ return careerStartYear; }
    public void setCareerStartYear(Integer careerStartYear){ this.careerStartYear = careerStartYear; }
    public MultipartFile getProfileImage(){ return profileImage; }
    public void setProfileImage(MultipartFile profileImage){ this.profileImage = profileImage; }
}
