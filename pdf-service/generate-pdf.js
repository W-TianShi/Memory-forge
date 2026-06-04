import puppeteer from 'puppeteer';
import { readFile, writeFile, unlink } from 'fs/promises';
import { existsSync } from 'fs';

async function main() {
  const htmlPath = process.argv[2];
  const pdfPath = process.argv[3];
  const landscape = process.argv[4] === 'landscape';

  if (!htmlPath || !pdfPath) {
    console.error('Usage: node generate-pdf.js <html-file> <pdf-output> [landscape]');
    process.exit(1);
  }

  if (!existsSync(htmlPath)) {
    console.error(`HTML file not found: ${htmlPath}`);
    process.exit(1);
  }

  let browser;
  try {
    const html = await readFile(htmlPath, 'utf-8');

    browser = await puppeteer.launch({
      headless: 'new',
      args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
    });

    const page = await browser.newPage();
    await page.setContent(html, {
      waitUntil: 'networkidle0',
      timeout: 15000
    });

    await page.pdf({
      path: pdfPath,
      format: 'A4',
      landscape: landscape,
      printBackground: true,
      preferCSSPageSize: true
      // margin is controlled by CSS @page rules in the HTML
    });

    console.log(`PDF generated: ${pdfPath}`);
  } catch (err) {
    console.error('PDF generation failed:', err.message);
    process.exit(1);
  } finally {
    if (browser) await browser.close();
  }
}

main();
