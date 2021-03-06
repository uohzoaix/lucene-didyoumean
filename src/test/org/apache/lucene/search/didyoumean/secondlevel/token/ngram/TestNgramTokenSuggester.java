package org.apache.lucene.search.didyoumean.secondlevel.token.ngram;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


import junit.framework.TestCase;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.index.facade.IndexWriterFacade;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.search.didyoumean.Suggestion;

import java.io.IOException;

/**
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-03
 *         Time: 06:27:55
 */
public class TestNgramTokenSuggester extends TestCase {

  // private static Log log = LogFactory.getLog(TestNgramTokenSuggester.class);
  // private static long serialVersionUID = 1l;

  private NgramTokenSuggester ngramTokenSuggester;
  private IndexFacade aprioriIndex;

  protected void setUp() throws Exception {
    super.setUp();

    //create a user index
    aprioriIndex = new DirectoryIndexFacade(new RAMDirectory());
    IndexWriterFacade writer = aprioriIndex.indexWriterFactory(new SimpleAnalyzer(), true);

    for (int i = 0; i < 1000; i++) {
      Document doc = new Document();
      doc.add(new Field("field1", intToEnglish(i), Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field("field2", intToEnglish(i + 1), Field.Store.YES, Field.Index.ANALYZED));
      writer.addDocument(doc);
    }


    writer.close();

    // create the spellChecker
    IndexFacade ngramIndex = new DirectoryIndexFacade(new RAMDirectory());
    ngramIndex.indexWriterFactory(null, true).close();
    ngramTokenSuggester = new NgramTokenSuggester(ngramIndex);
  }


  public void testBuild() {
    try {

      IndexReader reader = aprioriIndex.indexReaderFactory();

      addwords(reader, "field1");
      int num_field1 = ngramTokenSuggester.getNgramReader().numDocs();

      addwords(reader, "field2");
      int num_field2 = ngramTokenSuggester.getNgramReader().numDocs();


      assertEquals(num_field2, num_field1 + 1);

      // test small word
      assertEquals("five", ngramTokenSuggester.suggest("fvie", 2).top().getSuggested());
      assertEquals(0, ngramTokenSuggester.suggest("five", 2).size()); // Don't suggest self
      assertEquals("five", ((Suggestion)ngramTokenSuggester.suggest("fiv", 2).top()).getSuggested());
      assertEquals("five", ((Suggestion)ngramTokenSuggester.suggest("ive", 20).top()).getSuggested());
      assertEquals("five", ((Suggestion)ngramTokenSuggester.suggest("fives", 20).top()).getSuggested());
      assertEquals("five", ((Suggestion)ngramTokenSuggester.suggest("fie", 20).top()).getSuggested());
      assertEquals(0, ngramTokenSuggester.suggest("fi", 20).size());

      // test restraint to a field
      assertEquals(0, ngramTokenSuggester.suggest("tousand", 100, false, reader, "field1", false).size());
      assertEquals(1, ngramTokenSuggester.suggest("tousand", 100, false, reader, "field2", false).size());
      assertEquals("thousand", ngramTokenSuggester.suggest("tousand", 100, false, reader, "field2", false).top().getSuggested());

      // Test suggest self
      ngramTokenSuggester.setSuggestSelf(true);
      assertEquals(1, ngramTokenSuggester.suggest("five", 2).size());
      assertEquals("five", ngramTokenSuggester.suggest("five", 2).top().getSuggested());
      ngramTokenSuggester.setSuggestSelf(false);

    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }


  private void addwords(IndexReader r, String field) throws IOException {
    long time = System.currentTimeMillis();
    ngramTokenSuggester.indexDictionary(new TermEnumIterator(r, field));
    time = System.currentTimeMillis() - time;
    //System.out.println("time to build " + field + ": " + time);
  }


  public static String intToEnglish(int i) {
    StringBuffer result = new StringBuffer();
    intToEnglish(i, result);
    return result.toString();
  }

  public static void intToEnglish(int i, StringBuffer result) {
    if (i == 0) {
      result.append("zero");
      return;
    }
    if (i < 0) {
      result.append("minus ");
      i = -i;
    }
    if (i >= 1000000000) {			  // billions
      intToEnglish(i/1000000000, result);
      result.append("billion, ");
      i = i%1000000000;
    }
    if (i >= 1000000) {				  // millions
      intToEnglish(i/1000000, result);
      result.append("million, ");
      i = i%1000000;
    }
    if (i >= 1000) {				  // thousands
      intToEnglish(i/1000, result);
      result.append("thousand, ");
      i = i%1000;
    }
    if (i >= 100) {				  // hundreds
      intToEnglish(i/100, result);
      result.append("hundred ");
      i = i%100;
    }
    if (i >= 20) {
      switch (i/10) {
      case 9 : result.append("ninety"); break;
      case 8 : result.append("eighty"); break;
      case 7 : result.append("seventy"); break;
      case 6 : result.append("sixty"); break;
      case 5 : result.append("fifty"); break;
      case 4 : result.append("forty"); break;
      case 3 : result.append("thirty"); break;
      case 2 : result.append("twenty"); break;
      }
      i = i%10;
      if (i == 0)
        result.append(" ");
      else
        result.append("-");
    }
    switch (i) {
    case 19 : result.append("nineteen "); break;
    case 18 : result.append("eighteen "); break;
    case 17 : result.append("seventeen "); break;
    case 16 : result.append("sixteen "); break;
    case 15 : result.append("fifteen "); break;
    case 14 : result.append("fourteen "); break;
    case 13 : result.append("thirteen "); break;
    case 12 : result.append("twelve "); break;
    case 11 : result.append("eleven "); break;
    case 10 : result.append("ten "); break;
    case 9 : result.append("nine "); break;
    case 8 : result.append("eight "); break;
    case 7 : result.append("seven "); break;
    case 6 : result.append("six "); break;
    case 5 : result.append("five "); break;
    case 4 : result.append("four "); break;
    case 3 : result.append("three "); break;
    case 2 : result.append("two "); break;
    case 1 : result.append("one "); break;
    case 0 : result.append(""); break;
    }
  }



}
