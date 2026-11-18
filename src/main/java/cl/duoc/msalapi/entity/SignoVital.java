package cl.duoc.msalapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "signos_vitales")
public class SignoVital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "nombre_paciente")
    private String nombrePaciente;

    @Column(name = "correo_paciente")
    private String correoPaciente;

    @Column(name = "presion_sistolica")
    private Double presionSistolica;

    @Column(name = "presion_diastolica")
    private Double presionDiastolica;

    @Column(name = "frecuencia_cardiaca")
    private Double frecuenciaCardiaca;

    @Column(name = "frecuencia_respiratoria")
    private Double frecuenciaRespiratoria;

    @Column(name = "temperatura")
    private Double temperatura;

    @Column(name = "saturacion_oxigeno")
    private Double saturacionOxigeno;

    @Column(name = "fecha_hora")
    private String fechaHora;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String nombrePaciente) { this.nombrePaciente = nombrePaciente; }
    public String getCorreoPaciente() { return correoPaciente; }
    public void setCorreoPaciente(String correoPaciente) { this.correoPaciente = correoPaciente; }
    public Double getPresionSistolica() { return presionSistolica; }
    public void setPresionSistolica(Double presionSistolica) { this.presionSistolica = presionSistolica; }
    public Double getPresionDiastolica() { return presionDiastolica; }
    public void setPresionDiastolica(Double presionDiastolica) { this.presionDiastolica = presionDiastolica; }
    public Double getFrecuenciaCardiaca() { return frecuenciaCardiaca; }
    public void setFrecuenciaCardiaca(Double frecuenciaCardiaca) { this.frecuenciaCardiaca = frecuenciaCardiaca; }
    public Double getFrecuenciaRespiratoria() { return frecuenciaRespiratoria; }
    public void setFrecuenciaRespiratoria(Double frecuenciaRespiratoria) { this.frecuenciaRespiratoria = frecuenciaRespiratoria; }
    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }
    public Double getSaturacionOxigeno() { return saturacionOxigeno; }
    public void setSaturacionOxigeno(Double saturacionOxigeno) { this.saturacionOxigeno = saturacionOxigeno; }
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
}
