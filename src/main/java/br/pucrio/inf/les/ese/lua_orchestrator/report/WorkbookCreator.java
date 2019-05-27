package br.pucrio.inf.les.ese.lua_orchestrator.report;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

public class WorkbookCreator implements IWorkbookCreator {

	@Override
	public void create(Report report, String outputPath) throws IOException {

		Workbook wb = new HSSFWorkbook();

		for( br.pucrio.inf.les.ese.lua_orchestrator.report.Workbook workbook : report.getWorkbooks()){

			Sheet sheet = wb.createSheet(workbook.getName());

			// Create the header
			Row headerRow = sheet.createRow((short)0);
			// Put values
			for(int i=0;i<workbook.getHeaders().size();i++){
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(workbook.getHeaders().get(i));
			}

			// Put results now
			for(int i=0;i<workbook.getLines().size();i++){
				int index = i + 1;

				Row row = sheet.createRow((short)index);

				for(int column=0;column<workbook.getLines().get(i).size();column++){
					String cellValue = workbook.getLines().get(i).get(column);
					row.createCell(column).setCellValue(cellValue);
				}
			}

		}

		if(outputPath == null){
			outputPath = System.getProperty("user.dir");
		}

		String filename = buildFilename(outputPath,report.getName());

		// Write the output to a file
		FileOutputStream fileOut = new FileOutputStream(filename);
		wb.write(fileOut);

		fileOut.close();
		
		wb.close();
		
	}
	
	private String buildFilename(String outputPath, String basefilename){
		
		LocalDateTime now = LocalDateTime.now();
		int year = now.getYear();
		int month = now.getMonthValue();
		int day = now.getDayOfMonth();
		int hour = now.getHour();
		int minute = now.getMinute();
		int second = now.getSecond();
		int millis = now.get(ChronoField.MILLI_OF_SECOND); 
		
		String dateForFilename = year+"_"+month+"_"+day+"_"+hour+"_"+minute+"_"+second+"_"+millis;
		
		return outputPath+"\\"+basefilename+"_"+dateForFilename+".xls";
		
	}

}
