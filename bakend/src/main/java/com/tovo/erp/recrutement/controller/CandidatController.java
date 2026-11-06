package com.tovo.erp.recrutement.controller;

import com.tovo.erp.recrutement.dto.CandidatureDto;
import com.tovo.erp.recrutement.service.CandidatureService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidats")
public class CandidatController {

    private final CandidatureService candidatures;

    public CandidatController(CandidatureService candidatures) {
        this.candidatures = candidatures;
    }

    @GetMapping("/{idCandidat}/candidatures")
    public List<CandidatureDto> parCandidat(@PathVariable Long idCandidat) {
        return candidatures.parCandidat(idCandidat).stream().map(CandidatureDto::de).toList();
    }
}
