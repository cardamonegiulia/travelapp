package com.unical.travelapp.backend.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// Superclasse per il tracciamento "chi/quando ha creato o modificato per ultimo" una riga.
// Popolata automaticamente da Spring Data JPA Auditing (vedi JpaAuditingConfig), mai da
// codice applicativo o input del client.
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "creato_il", updatable = false)
    private LocalDateTime creatoIl;

    @LastModifiedDate
    @Column(name = "modificato_il")
    private LocalDateTime modificatoIl;

    @CreatedBy
    @Column(name = "creato_da", updatable = false, length = 100)
    private String creatoDa;

    @LastModifiedBy
    @Column(name = "modificato_da", length = 100)
    private String modificatoDa;
}
