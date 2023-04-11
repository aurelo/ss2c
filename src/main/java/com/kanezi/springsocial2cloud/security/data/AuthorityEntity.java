package com.kanezi.springsocial2cloud.security.data;

import com.kanezi.springsocial2cloud.security.LoginProvider;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authorities")
@Data
@NoArgsConstructor
public class AuthorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authority_id_generator")
    @SequenceGenerator(name = "authority_id_generator", sequenceName = "authorities_seq")
    Long id;

    String name;

    @Enumerated(value = EnumType.STRING)
    LoginProvider provider;

    @OneToMany(mappedBy = "authority")
    List<UserAuthorityEntity> assignedTo = new ArrayList<>();

    public AuthorityEntity(String name, LoginProvider provider) {
        this.name = name;
        this.provider = provider;
    }
}
