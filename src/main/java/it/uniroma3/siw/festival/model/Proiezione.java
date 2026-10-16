package it.uniroma3.siw.festival.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

// Indice su (sala_id, data): è esattamente il filtro della query di sovrapposizione
@Entity
@Table(indexes = @Index(name = "idx_proiezione_sala_data", columnList = "sala_id, data"))
public class Proiezione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate data;

    @NotNull
    private LocalTime ora;

    /*
     * Ora di fine = ora + durata del film, calcolata e salvata al momento della programmazione.
     * È un dato ridondante (si potrebbe ricavare dal film), ma averlo in tabella rende la query
     * di sovrapposizione semplice e indicizzabile: "inizio < altraFine AND fine > altraInizio",
     * senza dover fare join col film e aritmetica sulle date in JPQL.
     * Va ricalcolata se cambiano ora o film (vedi impostaOrario).
     */
    @NotNull
    private LocalTime oraFine;

    // STRING: nel DB finisce il nome ("SCHEDULED"), non la posizione nell'enum (ORDINAL),
    // così aggiungere o riordinare valori dell'enum non corrompe i dati esistenti
    @NotNull
    @Enumerated(EnumType.STRING)
    private StatoProiezione stato = StatoProiezione.SCHEDULED;

    // Tre @ManyToOne obbligatorie e LAZY (il default JPA sarebbe EAGER): il programma di un
    // festival si carica con una query JOIN FETCH dedicata quando servono film e sala
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    public Proiezione() {
    }

    /** Imposta data e ora di inizio e ricalcola l'ora di fine in base alla durata del film. */
    public void impostaOrario(LocalDate data, LocalTime ora) {
        this.data = data;
        this.ora = ora;
        this.oraFine = ora.plusMinutes(film.getDurata());
    }

    /** Solo le proiezioni ancora programmate possono essere modificate o annullate. */
    public boolean isModificabile() {
        return stato == StatoProiezione.SCHEDULED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getOra() {
        return ora;
    }

    public LocalTime getOraFine() {
        return oraFine;
    }

    public StatoProiezione getStato() {
        return stato;
    }

    public void setStato(StatoProiezione stato) {
        this.stato = stato;
    }

    public Festival getFestival() {
        return festival;
    }

    public void setFestival(Festival festival) {
        this.festival = festival;
    }

    public Film getFilm() {
        return film;
    }

    public void setFilm(Film film) {
        this.film = film;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Proiezione other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Proiezione.class.hashCode();
    }
}
