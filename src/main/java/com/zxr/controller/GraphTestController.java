package com.zxr.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class GraphTestController {
    private final CompiledGraph compiledGraph;

    public GraphTestController(CompiledGraph compiledGraph) {
        this.compiledGraph = compiledGraph;
    }

    @GetMapping("/graph")
    public String testGraph(@RequestParam String input){
        Optional<OverAllState> result = compiledGraph.invoke(
                Map.of("userInput",input)
        );
        return result.map(state -> (String)state.value("finalAnswer").orElse("unknown"))
                .orElse("no result");
    }
}
