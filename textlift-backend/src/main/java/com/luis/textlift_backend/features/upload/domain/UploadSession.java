package com.luis.textlift_backend.features.upload.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.luis.textlift_backend.features.auth.domain.User;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "upload_session"
)
public class UploadSession {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(nullable = false) @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    @Column
    private String originalFileName;

    @Column
    private String hash;

    public void setUploadStatus(UploadStatus status){
        this.uploadStatus = status;
    }
    public UUID getId(){
        return this.id;
    }
    public UploadStatus getUploadStatus(){
        return this.uploadStatus;
    }
    public String getOriginalFileName() {
        return originalFileName;
    }
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    public String getHash() {
        return this.hash;
    }
    public void setHash(String md5) {
        this.hash = md5;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }
}
