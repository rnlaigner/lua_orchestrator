package br.pucrio.inf.les.ese.lua_orchestrator.report;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface IWorkbookCreator {

	void create(Report report, String outputPath) throws IOException;
	
}
