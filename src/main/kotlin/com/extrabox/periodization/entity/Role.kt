package com.extrabox.periodization.entity

import jakarta.persistence.*

@Entity
@Table(name = "roles")
class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, unique = true)
    var name: String = ""

    @ManyToMany(mappedBy = "roles")
    var users: MutableSet<User> = mutableSetOf()

    // Construtor secundário
    constructor(id: Long? = null, name: String) {
        this.id = id
        this.name = name
    }

    // Construtor padrão sem argumentos necessário para o JPA
    constructor()

    companion object {
        const val ROLE_USER = "ROLE_USER"
        const val ROLE_ADMIN = "ROLE_ADMIN"
        const val ROLE_PREMIUM = "ROLE_PREMIUM"
    }
}
