package cl.duoc.msalapi.service;

import cl.duoc.msalapi.dto.SignoVitalDTO;
import cl.duoc.msalapi.entity.SignoVital;
import cl.duoc.msalapi.repository.SignoVitalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class SignoVitalService {

    private final SignoVitalRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public SignoVitalService(SignoVitalRepository repository,
                             KafkaTemplate<String, String> kafkaTemplate,
                             @Value("${app.kafka.topic-signos-vitales:signos-vitales}") String topic) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.topic = topic;
    }

    @Transactional
    public SignoVital registrar(SignoVitalDTO dto) {
        SignoVital entidad = new SignoVital();
        entidad.setPacienteId(dto.getPacienteId());
        entidad.setNombrePaciente(dto.getNombrePaciente());
        entidad.setCorreoPaciente(dto.getCorreoPaciente());
        entidad.setPresionSistolica(dto.getPresionSistolica());
        entidad.setPresionDiastolica(dto.getPresionDiastolica());
        entidad.setFrecuenciaCardiaca(dto.getFrecuenciaCardiaca());
        entidad.setFrecuenciaRespiratoria(dto.getFrecuenciaRespiratoria());
        entidad.setTemperatura(dto.getTemperatura());
        entidad.setSaturacionOxigeno(dto.getSaturacionOxigeno());
        entidad.setFechaHora(dto.getFechaHora());

        SignoVital guardado = repository.save(entidad);

        try {
            String valor = objectMapper.writeValueAsString(guardado);
            kafkaTemplate.send(topic, String.valueOf(guardado.getPacienteId()), valor);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo publicar el signo vital a Kafka", ex);
        }

        return guardado;
    }

    public List<SignoVital> listarPorPaciente(Long pacienteId) {
        return repository.findByPacienteIdOrderByIdDesc(pacienteId);
    }
}
