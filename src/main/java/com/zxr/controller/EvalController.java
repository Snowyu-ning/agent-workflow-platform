package com.zxr.controller;

import com.zxr.evaluation.EvalRunner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/eval")
public class EvalController {

    private final EvalRunner evalRunner;

    public EvalController(EvalRunner evalRunner) {
        this.evalRunner = evalRunner;
    }

    @PostMapping("/run")
    public Map<String, Object> run() throws Exception {
        return evalRunner.runEvaluation();
    }
}