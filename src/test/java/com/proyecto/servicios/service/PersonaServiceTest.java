package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonaService personaService;

    @Test
    @DisplayName("Debe crear persona exitosamente")
    void testCreaPersona() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Juan");
        request.setApellidoP("Pérez");
        request.setApellidoMaterno("López");

        when(personasRepository.save(any(Personas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonaResponse response = personaService.creaPersona(request);

        assertNotNull(response);
        assertEquals(1, response.getCodigo());
        assertEquals("Exito", response.getMensaje());
        verify(personasRepository, times(1)).save(any(Personas.class));
    }

    @Test
    @DisplayName("Debe eliminar persona cuando existe")
    void testEliminaPersonaExiste() {
        EliminaPersonaRequest req = new EliminaPersonaRequest();
        req.setNombre("Juan");

        Personas persona = new Personas();
        persona.setNombre("Juan");
        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(persona));

        GenericResponse resp = personaService.eliminaPersona(req);
        assertEquals(0, resp.getCodigo());
        assertTrue(resp.getMensaje().contains("eliminada"));
        verify(personasRepository, times(1)).delete(persona);
    }

    @Test
    @DisplayName("Debe retornar código 1 al eliminar si la persona no existe")
    void testEliminaPersonaNoExiste() {
        EliminaPersonaRequest req = new EliminaPersonaRequest();
        req.setNombre("Inexistente");
        when(personasRepository.findByNombre("Inexistente")).thenReturn(Optional.empty());

        GenericResponse resp = personaService.eliminaPersona(req);
        assertEquals(1, resp.getCodigo());
        verify(personasRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe actualizar persona cuando existe")
    void testActualizaPersonaExiste() {
        PersonasRequest req = new PersonasRequest();
        req.setNombre("Juan");
        req.setApellidoP("Gómez");
        req.setApellidoMaterno("Díaz");

        Personas persona = new Personas();
        persona.setNombre("Juan");
        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(persona));
        when(personasRepository.save(any(Personas.class))).thenReturn(persona);

        GenericResponse resp = personaService.actualizaPersona(req);
        assertEquals(0, resp.getCodigo());
        assertEquals("Actualizado correctamente", resp.getMensaje());
        verify(personasRepository, times(1)).save(persona);
    }

    @Test
    @DisplayName("Debe retornar código 1 al actualizar si la persona no existe")
    void testActualizaPersonaNoExiste() {
        PersonasRequest req = new PersonasRequest();
        req.setNombre("Inexistente");
        when(personasRepository.findByNombre("Inexistente")).thenReturn(Optional.empty());

        GenericResponse resp = personaService.actualizaPersona(req);
        assertEquals(1, resp.getCodigo());
        verify(personasRepository, never()).save(any());
    }
}

