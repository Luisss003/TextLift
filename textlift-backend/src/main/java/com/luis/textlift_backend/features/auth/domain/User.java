package com.luis.textlift_backend.features.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.luis.textlift_backend.features.upload.domain.UploadSession;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL
    )
    @JsonManagedReference
    private List<UploadSession> uploadSessions;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
    private Role role;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column
    private boolean enabled;

    @Column
    private String verificationCode;

    @Column
    private LocalDateTime verificationExpiration;


    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User() {
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.getName().toString());
        return List.of(authority);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    public void setEmail(String email){
        this.email = email;
    }
    public void setPassword(String password){
        this.password = password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEnabled(boolean b) {
        this.enabled = b;
    }

    public UUID getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<UploadSession> getUploadSessions() {
        return uploadSessions;
    }

    public void setUploadSessions(List<UploadSession> uploadSessions) {
        this.uploadSessions = uploadSessions;
    }
}


