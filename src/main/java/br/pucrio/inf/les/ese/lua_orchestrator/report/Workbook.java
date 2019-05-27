package br.pucrio.inf.les.ese.lua_orchestrator.report;

import java.util.ArrayList;
import java.util.List;

public class Workbook {

    private String name;

    private List<String> headers;

    private List<List<String>> lines;

    public Workbook(){
        this.lines = new ArrayList<List<String>>();
    }

    public Workbook(String name, List<String> headers) {
        this.name = name;
        this.headers = headers;
        this.lines = new ArrayList<List<String>>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<String>> getLines() {
        return lines;
    }

    public void setLines(List<List<String>> lines) {
        this.lines = lines;
    }

    public void addLine(List<String> line){
        this.lines.add(line);
    }

}
