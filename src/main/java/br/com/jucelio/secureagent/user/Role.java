package br.com.jucelio.secureagent.user;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    protected Role() {}

    public Role(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
}
