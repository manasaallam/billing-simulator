package com.example.billingsimulator.controller;

import com.example.billingsimulator.dto.SimulationRequest;
import com.example.billingsimulator.dto.SimulationRequestDto;
import com.example.billingsimulator.service.GroqService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {

    private final GroqService groqService;

    public SimulationController(GroqService groqService) {
        this.groqService = groqService;
    }

    @PostMapping
    public SimulationRequest simulate(@RequestBody SimulationRequestDto request) throws Exception {

        return groqService.extractParameters(request.question());

    }
}

