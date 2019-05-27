package br.pucrio.inf.les.ese.lua_orchestrator.report;

import java.util.ArrayList;
import java.util.List;

public class Report {

    private String name;

	private List<Workbook> workbooks;

    public Report() {
        this.workbooks = new ArrayList<Workbook>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Workbook> getWorkbooks() {
		return workbooks;
	}

	public void addWorkbook(Workbook workbook) {
		this.workbooks.add(workbook);
	}
}
