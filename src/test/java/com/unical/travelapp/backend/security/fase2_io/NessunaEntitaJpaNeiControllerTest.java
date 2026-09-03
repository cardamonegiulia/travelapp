package com.unical.travelapp.backend.security.fase2_io;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NessunaEntitaJpaNeiControllerTest {

    private static final String PACKAGE_BASE = "com.unical.travelapp.backend";

    private List<Class<?>> controller() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> classi = new ArrayList<>();
        for (BeanDefinition definizione : scanner.findCandidateComponents(PACKAGE_BASE)) {
            try {
                classi.add(Class.forName(definizione.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return classi;
    }

    @Test
    void ilProgettoEspoOneAlmenoUnRestControllerDaControllare() {
        assertThat(controller())
                .as("lo scanner deve trovare i controller del progetto")
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void nessunMetodoDiControllerRestituisceUnEntitaJpa() {
        List<String> violazioni = new ArrayList<>();

        for (Class<?> classe : controller()) {
            for (Method metodo : classe.getDeclaredMethods()) {
                if (metodo.isSynthetic()) {
                    continue;
                }
                for (Class<?> tipo : tipiCoinvolti(metodo.getGenericReturnType())) {
                    if (eEntitaJpa(tipo)) {
                        violazioni.add(classe.getSimpleName() + "#" + metodo.getName()
                                + " restituisce l'entita' " + tipo.getSimpleName());
                    }
                }
            }
        }

        assertThat(violazioni).isEmpty();
    }

    @Test
    void nessunMetodoDiControllerAccettaUnEntitaJpaInIngresso() {
        List<String> violazioni = new ArrayList<>();

        for (Class<?> classe : controller()) {
            for (Method metodo : classe.getDeclaredMethods()) {
                if (metodo.isSynthetic()) {
                    continue;
                }
                for (Parameter parametro : metodo.getParameters()) {
                    for (Class<?> tipo : tipiCoinvolti(parametro.getParameterizedType())) {
                        if (eEntitaJpa(tipo)) {
                            violazioni.add(classe.getSimpleName() + "#" + metodo.getName()
                                    + " accetta l'entita' " + tipo.getSimpleName());
                        }
                    }
                }
            }
        }

        assertThat(violazioni).isEmpty();
    }

    @Test
    void ogniCorpoDiRichiestaEUnDtoDedicato() {
        List<String> violazioni = new ArrayList<>();

        for (Class<?> classe : controller()) {
            for (Method metodo : classe.getDeclaredMethods()) {
                for (Parameter parametro : metodo.getParameters()) {
                    if (!parametro.isAnnotationPresent(RequestBody.class)) {
                        continue;
                    }
                    for (Class<?> tipo : tipiCoinvolti(parametro.getParameterizedType())) {
                        if (eEntitaJpa(tipo)) {
                            violazioni.add(classe.getSimpleName() + "#" + metodo.getName()
                                    + ": @RequestBody sull'entita' " + tipo.getSimpleName());
                        }
                    }
                }
            }
        }

        assertThat(violazioni).isEmpty();
    }

    @Test
    void ilControlloRiconosceDavveroUnEntitaJpa() {
        assertThat(eEntitaJpa(com.unical.travelapp.backend.identity.entity.Utente.class)).isTrue();
        assertThat(eEntitaJpa(com.unical.travelapp.backend.booking.entity.Prenotazione.class)).isTrue();

        Type tipoAnnidato = new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() {
                return new Type[]{com.unical.travelapp.backend.identity.entity.Utente.class};
            }
            @Override public Type getRawType() { return ResponseEntity.class; }
            @Override public Type getOwnerType() { return null; }
        };
        assertThat(tipiCoinvolti(tipoAnnidato))
                .as("il rilevatore deve guardare dentro i generici")
                .contains(com.unical.travelapp.backend.identity.entity.Utente.class);
    }

    private boolean eEntitaJpa(Class<?> tipo) {
        return tipo.isAnnotationPresent(Entity.class);
    }

    private Set<Class<?>> tipiCoinvolti(Type tipo) {
        Set<Class<?>> risultato = new LinkedHashSet<>();
        raccogli(tipo, risultato);
        return risultato;
    }

    private void raccogli(Type tipo, Set<Class<?>> accumulatore) {
        if (tipo instanceof Class<?> classe) {
            accumulatore.add(classe);
        } else if (tipo instanceof ParameterizedType parametrizzato) {
            raccogli(parametrizzato.getRawType(), accumulatore);
            for (Type argomento : parametrizzato.getActualTypeArguments()) {
                raccogli(argomento, accumulatore);
            }
        } else if (tipo instanceof WildcardType jolly) {
            for (Type limite : jolly.getUpperBounds()) {
                raccogli(limite, accumulatore);
            }
        }
    }
}
