package semanticRelatedness;

/* Copyright 2011 Manuel Wahle
 *
 * This file is part of Hello-Wiki.
 *
 *    Hello-Wiki is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Hello-Wiki is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Hello-Wiki.  If not, see <http://www.gnu.org/licenses/>.
 */



import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * This class is a parser for a Wikipedia dump file in XML format ("wikidump"). It was written for the dump files that contain the complete article source. They
 * are named like <code>enwiki-latest-pages-articles.xml.bz2</code>. You can get the latest Wikipedia dump in a single file at <a
 * href="http://dumps.wikimedia.org/enwiki/latest" target="_blank">dumps.wikimedia.org/enwiki/latest</a> for the English language. Dumps for specific dates are
 * also available, where "latest" is replaced with a date string like "20110115" for example. <p>
 *
 * The philosophy of this class is to work like a unidirectional iterator through the articles in the dump. The constructor needs as its only argument the name
 * of the dump file with an absolute or a relative path from the current working directory. <p>
 *
 * Subsequent calls to the <code>nextPage()</code> method let you iterate through the pages (the Wikipedia pagesm which may be of different types). For each
 * page, you can extract text, links to other articles, page type (actual article, redirect page, template, ...) and other information from the current
 * article's markup. For some fields, a separator can be chosen (other than a white space). This is useful because, for example, internal Wikipedia links (to
 * other articles) can contain white spaces. <p>
 *
 * For the extraction of information, there are two public methods with the same name, the philosophy of which is the following. One method has a Boolean
 * argument, the other method none. The Boolean is to indicate if the requested data should be freshly extracted from the current page source or come from
 * cache. For example, <code>getPageTitle(true)</code> will processes the current page source and cache the result. If the title is requested a second time, the
 * method <code>getPageTitle(false)</code> could be used to only return the cached data without reprocessing the current page source a second time, saving some
 * computations. For convenience, the method without an argument (<code>getPageTitle()</code> in this example) will simply call its sibling with the Boolean
 * argument set to true. This is what most users will normally use.
 * 
 * @author mwahle
 */
public class Extractor {
    /**
     * An enum to indicate the page type.
     */
    public static enum PageType {
        /** The default value. Also set when determining the page type failed. */
        UNKNOWN,
        /** The page is a regular Wikipedia article. */
        ARTICLE,
        /** The page is a redirect page. */
        REDIRECT,
        /** The page is a disambiguation page. */
        DISAMBIGUATION,
        /** The page is a Wikipedia page. (What's that?) */
        WIKIPEDIA,
        /** The page is a file. (What's that?) */
        FILE,
        /** The page is a template. */
        TEMPLATE,
        /** The page is a category listing. */
        CATEGORY,
        /** The page is a portal. */
        PORTAL
    };

    private PageType _currentPageType = PageType.UNKNOWN;
    private boolean _currentPageIsStub = false;

    private String _titleSeparator = "_";
    private String _categorySeparator = "_";
    private String _categoryListSeparator = " ";
    private String _linkSeparator = "_";
    private String _linkListSeparator = " ";
    private String _dumpfileName;
    private BufferedReader _dumpfileReader;

    private String _currentPageSource = new String();
    private String _currentPageTitle = new String();
    private ArrayList<String> _currentPageLinks = new ArrayList<String>();
    private ArrayList<String> _currentPageCategories = new ArrayList<String>();
    private String _currentPageText = new String();
    private String _currentPageAbstract = new String();
    private long _articleId = 0;

    /**
     * Creates a new <code>Extractor</code> instance.
     * 
     * @param dumpfileName <code>String</code> value with the name of the wikidump file
     */
    public Extractor(String dumpfileName) {
        _dumpfileName = dumpfileName;

        // set up input reader
        try {
        	FileInputStream fin = new FileInputStream(_dumpfileName);
        	BufferedInputStream in = new BufferedInputStream(fin);
        	//FileOutputStream out = new FileOutputStream("archive.tar");
        	BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);

            _dumpfileReader = new BufferedReader(new InputStreamReader(bzIn));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Forward-iterates through the individual pages in the dumpfile. If it doesn't return true, it couldn't find the next page, which means that the xml file
     * may be broken or the end of it is reached. To start the browsing from the beginning, a new instance of this class must be created.<p>
     *
     * Before it is called the first time, <code>nextPage</code> points to an empty page.
     * 
     * @return <code>boolean</code> false if no next page was found, true otherwise
     */
    public boolean nextPage() {
        extractPageSource();

        _currentPageType = PageType.UNKNOWN;
        _currentPageIsStub = false;
        _currentPageTitle = "";
        _currentPageLinks.clear();
        _currentPageCategories.clear();
        _currentPageText = "";
        _currentPageAbstract = "";

        return (!_currentPageSource.isEmpty());
    }

    /**
     * Returns a string containing the unprocessed wiki markup code for the current page.
     * 
     * @return String with the current page source
     */
    public String getPageSource() {
        return _currentPageSource;
    }

    /**
     * Convenience wrapper for {@link #getPageText(boolean)}, always passing true so that the extraction process is initiated.  This is the function to be used
     * normally (when the article text is requested the first (or the only) time).
     * 
     * @return String with the current page text, stripped of markup tags
     */
    public String getPageText() {
        return getPageText(true);
    }

    /**
     * Returns the text of the current page. With the Boolean argument set to true, it will extract, clean, cache, and return the data.  If false is passed in,
     * the cached text will be returned only (note: immediately after calling <code>nextPage</code>, this cache is empty), saving some redundant calculations.
     * 
     * @param bExtract if true, the text is extracted from the page source, and cleaned and cached before returned. If false, the cached text will be returned
     *        only
     * @return String with the current page text, stripped of markup tags
     */
    public String getPageText(boolean bExtract) {
        if (bExtract)
            extractPageText();

        return _currentPageText;
    }

    /**
     * Private method to do the actual data extraction from the page source. Caches the result in a private String.
     */
    // TODO: some formatting like removal of excessive whitespaces?
    private void extractPageText() {
        _currentPageText = "";

        if (_currentPageSource.isEmpty())
            return;

        int iStartIndex = _currentPageSource.indexOf("<text ");
        if (iStartIndex < 0)
            return;

        iStartIndex = _currentPageSource.indexOf(">", iStartIndex);
        ++iStartIndex;

        int iEndIndex = _currentPageSource.indexOf("</text>");
        if (iEndIndex < 0)
            return;

        _currentPageText = _currentPageSource.substring(iStartIndex, iEndIndex);

        // find the the external links section, and cut it away
        iStartIndex = _currentPageText.indexOf("External Links");
        if (iStartIndex < 0)
            iStartIndex = _currentPageText.indexOf("External links");
        if (iStartIndex > 0) {
            iEndIndex = _currentPageText.indexOf("\n");

            if (iEndIndex < 0)
                _currentPageText = _currentPageText.substring(0, iStartIndex);
            else {
                String tmp = _currentPageText = _currentPageText.substring(0, iStartIndex);
                _currentPageText = tmp + _currentPageText.substring(iEndIndex, _currentPageText.length() - 1);
            }
        }

        _currentPageText = filterTags(_currentPageText);
        
        String[] words = _currentPageText.split(" ");
        _currentPageText = "";
        String space = " ";
        Set<String> wordSet = new HashSet<String>(Arrays.asList(words));
        for(String w : words) {
        	w = w.toLowerCase();
        	if(stopWordSet.contains(w)){
        		wordSet.remove(w);
        		continue;
        	}
        	_currentPageText += space;
        	_currentPageText += w;
        }        
        if(wordSet.size() < 100) {
        	_currentPageText = "";
        	return;
        }
    }

    /**
     * Convenience wrapper for {@link #getPageAbstract(boolean)}, always passing true so that the extraction process is initiated. This is the function to be
     * used normally (when the article abstract is requested the first (or the only) time).
     * 
     * @return String with the current page abstract, stripped of markup tags
     */
    public String getPageAbstract() {
        return getPageAbstract(true);
    }

    /**
     * Returns the abstract of the current page. With the Boolean argument set to true, it will extract, clean, cache, and return the data. If false is passed
     * in, the cached abstract will be returned only (note: immediately after calling <code>nextPage</code>, this cache is empty), saving some redundant
     * calculations.
     * 
     * @param bExtract if true, the abstract is extracted from the page source, and cleaned and cached before returned. If false, the cached abstract will be
     *        returned only
     * @return String with the current page abstract, stripped of markup tags
     */
    public String getPageAbstract(boolean bExtract) {
        if (bExtract)
            extractPageAbstract();

        return _currentPageAbstract;
    }

    /**
     * Private method to do the actual data extraction from the page source. Caches the result in a private variable.
     */
    private void extractPageAbstract() {
        _currentPageAbstract = "";

        if (_currentPageSource.isEmpty())
            return;

        int iStartIndex = _currentPageSource.indexOf("<text ");
        if (iStartIndex < 0)
            return;

        iStartIndex = _currentPageSource.indexOf(">", iStartIndex);
        ++iStartIndex;

        int iEndIndex = _currentPageSource.indexOf("==", iStartIndex);

        if (iEndIndex < 0) {
            iEndIndex = _currentPageSource.indexOf("</text>", iStartIndex);
            if (iEndIndex < 0)
                return;
        }

        _currentPageAbstract = _currentPageSource.substring(iStartIndex, iEndIndex);

        _currentPageAbstract = filterTags(_currentPageAbstract);
    }

    /**
     * Private method to filter away markup tags
     * 
     * @param string the <code>String</code> with the markup code to be filtered
     * @return a <code>String</code> with the filtered text
     */
    private String filterTags(String string) {

        String cleanedString = string;

        // remove all between curly brackets
        cleanedString = cleanedString.replaceAll("\\{\\{[^\\}\\}]*\\}\\}", " ");

        // remove tags like [[en:English]] and [[File:http://foo.bar]]
        cleanedString = cleanedString.replaceAll("\\[\\[[^\\]]+:[^\\]]+\\]\\]", " ");

        // references ...
//        cleanedString = cleanedString.replaceAll("\\&lt;ref", "<<<<<");
//        cleanedString = cleanedString.replaceAll("/ref\\&gt;", ">>>>>");

        // math ...
//        cleanedString = cleanedString.replaceAll("<math>", "<<<<<");
//        cleanedString = cleanedString.replaceAll("</math>", ">>>>>");

        // html tags ...
     //   cleanedString = cleanedString.replaceAll("\\&lt;(?!.*\\&gt;).*\\&gt;", " ");
//        cleanedString = cleanedString.replaceAll("\\&gt;", ">>>>>");

        // ... and remove 'em
//        cleanedString = cleanedString.replaceAll("<<<<<[^>>>>>]*>>>>>", " ");

        // replace [[word|link]] with link
        cleanedString = cleanedString.replaceAll("\\[\\[[^\\]]+\\|([^\\]]+)\\]\\]", " $1 ");

        // replace [[word#link]] with link
        cleanedString = cleanedString.replaceAll("\\[\\[[^\\]]+#([^\\]]+)\\]\\]", " $1 ");

        // replace [[foo]] with foo
        cleanedString = cleanedString.replaceAll("\\[\\[([^\\]\\]]*)\\]\\]", " $1 ");

        // remove [http://...] references
        cleanedString = cleanedString.replaceAll("\\[http[^\\]]*\\]", " ");

        // some trivia...
//        cleanedString = cleanedString.replaceAll("=====", " ");
//        cleanedString = cleanedString.replaceAll("====", " ");
//        cleanedString = cleanedString.replaceAll("===", " ");
//        cleanedString = cleanedString.replaceAll("==", " ");
//        cleanedString = cleanedString.replaceAll("'''''", " ");
//        cleanedString = cleanedString.replaceAll("''''", " ");
//        cleanedString = cleanedString.replaceAll("'''", " ");
//        cleanedString = cleanedString.replaceAll("''", " ");
        cleanedString = cleanedString.replaceAll("\\&quot;", " ");
//        cleanedString = cleanedString.replaceAll("\\&amp;", " and ");
        cleanedString = cleanedString.replaceAll("\\&ndash;", " ");
//        cleanedString = cleanedString.replaceAll("\\&nbsp;", " ");
        cleanedString = cleanedString.replaceAll("\\&[^\\&]*;", " ");

        cleanedString = cleanedString.replaceAll("[^\\w]", " ");
        cleanedString = cleanedString.replaceAll("\\b\\w{1,2}\\b", " ");
        cleanedString = cleanedString.replaceAll("\\s+", " ");

//        String strTokenSplit = " \t\n\r`~!@#$%^&*()_=+|[;]{},./?<>:’\\\"";
//        cleanedString = cleanedString.replaceAll(" ([\\.\\,\\;])", "$1");
        return cleanedString;
    }

    /**
     * Convenience wrapper for {@link #getStub(boolean)}, always passing true so that the extraction process is initiated. This is the
     * function to be used normally (when the information is requested the first (or the only) time).
     * 
     * @return boolean true if the current page is a stub, false otherwise
     */
    public boolean getStub() {
        return getStub(true);
    }

    /**
     * Returns true if it was determined that the current page is a stub. It is a stub if a wiki markup tag for a stub is present in the page source. With the
     * Boolean argument set to true, it will extract the information from the current page source. If false is passed in, the cached value will be returned only
     * 
     * @param bExtract boolean to indicate if processing of the source is desired
     * @return boolean true if the current page is a stub, false otherwise
     */
    public boolean getStub(boolean bExtract) {
        if (bExtract)
            extractStub();

        return _currentPageIsStub;
    }

    /**
     * Private method to parse the page source and determine if it is a stub. Caches the result in a private variable.
     */
    private void extractStub() {
        if (!_currentPageSource.isEmpty() && _currentPageSource.contains("-stub}}"))
            _currentPageIsStub = true;
        else
            _currentPageIsStub = false;
    }

    /**
     * Convenience wrapper for {@link #getPageTitle(boolean)}, always passing true so that the extraction process is initiated.  This is the function to be used
     * normally (when the article title is requested the first (or the only) time).
     * 
     * @return String with the current page title, stripped of markup tags
     */
    public String getPageTitle() {
        return getPageTitle(true);
    }

    /**
     * Returns the title of the current page. With the Boolean argument set to true, it will extract, clean, cache, and return the data. If false is passed in,
     * the cached title will be returned only (note: immediately after calling <code>nextPage</code>, this cache is empty), saving some redundant
     * calculations. <p>
     *
     * Words withing the title are separated by default with an underscore. This is handy if the title should serve as a unique identifier. However, it may be
     * desirable to use another separator. {@link #setTitleSeparator(String)} and {@link #getTitleSeparator()} allow to set and query the current separator.
     * 
     * @param bExtract if true, the title is extracted from the page source, and cleaned and cached before returned. If false, the cached title will be returned
     *        only
     * @return String with the current page title, stripped of markup tags
     */
    public String getPageTitle(boolean bExtract) {
        if (bExtract)
            extractPageTitle();

        if (_titleSeparator.equals(" "))
            return _currentPageTitle;
        else
            return _currentPageTitle.replace(" ", _titleSeparator);
    }

    /**
     * Private method to do the actual data extraction from the page source. Caches the result in a private variable.
     */
    private void extractPageTitle() {
        _currentPageTitle = "";

        if (!_currentPageSource.isEmpty()) {
            try {
                _currentPageTitle = _currentPageSource.substring(_currentPageSource.indexOf("<title>") + 7, _currentPageSource.indexOf("</title>"));
            } catch (Exception e) {
                e.printStackTrace();
                _currentPageTitle = "";
            }
        }
    }
    
    public long getPageId() {
        extractPageId();
        return _articleId;

    }
    
    private void extractPageId() {
    	_articleId = 0;

        if (!_currentPageSource.isEmpty()) {
            try {
            	_articleId = Long.parseLong(_currentPageSource.substring(_currentPageSource.indexOf("<id>") + 4, _currentPageSource.indexOf("</id>")));
            } catch (Exception e) {
                e.printStackTrace();
                _currentPageTitle = "";
            }
        }
    }
    

    /**
     * Convenience wrapper for {@link #getPageCategories(boolean)}, always passing true so that the extraction process is initiated. This is the function to be
     * used normally (when the article categories are requested the first (or the only) time).
     * 
     * @return String with the current page categories, stripped of markup tags
     */
    public String getPageCategories() {
        return getPageCategories(true);
    }

    /**
     * Returns the categories of the current page as a Java <code>ArrayList\<String\></code>. With the Boolean argument set to true, it will extract, * clean,
     * cache, and return the data. If false is passed in, the cached categories will be returned only (note: immediately after calling * <code>nextPage</code>,
     * this cache is empty), saving some redundant calculations. <p>
     *
     * Words withing a category are separated by default with an underscore. This is handy when they should serve as unique links to other Wikipedia
     * articles. However, it may be desirable to use another separator. {@link #setCategoryListSeparator(String)} and {@link #getCategoryListSeparator()} allow
     * to set and query the current separator.
     * 
     * @param bExtract if true, the categories are extracted and cached from the page source before returned. If false, the cached categories will be returned
     *        only
     * @return ArrayList<String> with the current page categories, stripped of markup tags
     */
    // TODO: add defitition of categories
    public ArrayList<String> getPageCategoriesList(boolean bExtract) {
        if (bExtract)
            extractPageCategories();

        return _currentPageCategories;
    }

    /**
     * Returns the categories of the current page as single string. With the Boolean argument set to true, it will extract, * clean, cache, and return the
     * data. If false is passed in, the cached categories will be returned only (note: immediately after calling * <code>nextPage</code>, this cache is empty),
     * saving some redundant calculations. <p>
     *
     * Words withing a category are separated by default with an underscore. This is handy when they should serve as unique links to other Wikipedia
     * articles. However, it may be desirable to use another separator. {@link #setCategoryListSeparator(String)} and {@link #getCategoryListSeparator()} allow
     * to set and query the current separator.  Depending on this separator within links, an appropriate character sequence that separates the links from each
     * other must be chosen, which can be done with {@link #setCategoryListSeparator(String)} and {@link #getCategoryListSeparator()}. By default, this is the
     * whitespace character.  {@link #getPageCategoryList(boolean)} returns the categories as a Java <code>ArrayList\<String\></code>.
     * 
     * @param bExtract if true, the categories are extracted and cached from the page source before returned. If false, the cached categories will be returned
     *        only
     * @return String with the current page categories, stripped of markup tags
     */
    // TODO: add defitition of categories
    public String getPageCategories(boolean bExtract) {
        if (bExtract)
            extractPageCategories();

        String pageCategories = new String();

        if (_currentPageCategories.size() > 0) {
            for (int i = 0; i < _currentPageCategories.size(); ++i)
                pageCategories = pageCategories.concat(_currentPageCategories.get(i).replaceAll("%s", _categorySeparator).concat(_categoryListSeparator));
            pageCategories = pageCategories.substring(0, pageCategories.length() - 1);
        }

        return pageCategories;
    }

    /**
     * Returns the categories of the current page as a Java <code>ArrayList\<String\></code>. With the Boolean argument set to true, it will extract, * clean,
     * cache, and return the data. If false is passed in, the cached categories will be returned only (note: immediately after calling * <code>nextPage</code>,
     * this cache is empty), saving some redundant calculations. <p>
     *
     * Words withing a category are separated by default with an underscore. This is handy when they should serve as unique categories to other Wikipedia
     * articles. However, it may be desirable to use another separator. {@link #setCategoryListSeparator(String)} and {@link #getCategoryListSeparator()} allow
     * to set and query the current separator.
     * 
     * @param bExtract if true, the categories are extracted and cached from the page source before returned. If false, the cached categories will be returned
     * only
     * @return ArrayList<String> with the current page categories, stripped of markup tags
     */
    // TODO: add defitition of categories
    public ArrayList<String> getPageCategoryList(boolean bExtract) {
        if (bExtract)
            extractPageCategories();

        return _currentPageCategories;
    }

    /**
     * Private method to do the actual data extraction from the page source. Caches the result in a private variable.
     */
    private void extractPageCategories() {
        _currentPageCategories.clear();

        int iStartIndex = _currentPageSource.indexOf("[[Category:");
        if (iStartIndex < 0)
            return;

        String body = _currentPageSource.substring(iStartIndex);

        String[] categories = body.split("\\[\\[Category:");

        boolean bFirstLoop = true;

        for (String category : categories) {
            if (bFirstLoop == true) {
                bFirstLoop = false;
                continue;
            }

            int iEndIndex = category.indexOf("]]");
            if (iEndIndex < 0)
                continue;

            category = category.substring(0, iEndIndex);

            /*
             * if ( category.contains( "&lt;" ) ) { category = category.replaceAll( "&lt;", "<<<<<" ); category = category.replaceAll( "&gt;", ">>>>>" );
             * category = category.replaceAll( "<<<<<[^>>>>>]*>>>>>", " " ); }
             * 
             * if ( category.contains( "&quot" ) ) { iStartIndex = category.indexOf( "&quot;" ); iEndIndex = category.indexOf( "&quot;", iStartIndex ); category
             * = category.substring( iStartIndex, iEndIndex + 6 ); }
             */

            if (category.contains("#"))
                category = category.substring(0, category.indexOf("#"));
            else if (category.contains("|"))
                category = category.substring(0, category.indexOf("|"));
            if (stopCategoriesSet.contains(category)) {
            	_currentPageCategories.clear();
            	return;
            }
            
            _currentPageCategories.add(category);
        }
    }

    /**
     * Convenience wrapper for {@link #getPageLinks(boolean)}, always passing true so that the extraction process is initiated.  This is the function to be used
     * normally (when the article links are requested the first (or the only) time).
     * 
     * @return String with the current page links, stripped of markup tags
     */
    public String getPageLinks() {
        return getPageLinks(true);
    }

    /**
     * Returns the links of the current page as a Java <code>ArrayList\<String\></code>. With the Boolean argument set to true, it will extract, * clean, cache,
     * and return the data. If false is passed in, the cached links will be returned only (note: immediately after calling * <code>nextPage</code>, this cache
     * is empty), saving some redundant calculations. <p>
     *
     * Words withing a category are separated by default with an underscore. This is handy when they should serve as unique links to other Wikipedia
     * articles. However, it may be desirable to use another separator. {@link #setLinkListSeparator(String)} and {@link #getLinkListSeparator()} allow to set
     * and query the current separator.
     * 
     * @param bExtract if true, the links are extracted and cached from the page source before returned. If false, the cached links will be returned only
     * @return ArrayList<String> with the current page links, stripped of markup tags
     */
    // TODO: add defitition of links 
    public ArrayList<String> getPageLinkList(boolean bExtract) {
        if (bExtract)
            extractPageLinks();

        return _currentPageLinks;
    }

    /**
     * Returns the links of the current page as single string. With the Boolean argument set to true, it will extract, * clean, cache, and return the data. If
     * false is passed in, the cached links will be returned only (note: immediately after calling * <code>nextPage</code>, this cache is empty), saving some
     * redundant calculations. <p>
     * 
     * Words withing a category are separated by default with an underscore. This is handy when they should serve as unique links to other Wikipedia
     * articles. However, it may be desirable to use another separator.  {@link #setLinkListSeparator(String)} and {@link #getLinkListSeparator()} allow to
     * set and query the current separator. Depending on this separator within links, an appropriate character sequence that separates the links from each other
     * must be chosen, which can be done with {@link #setLinkListSeparator(String)} and {@link #getLinkListSeparator()}. By default, this is the whitespace
     * character.  {@link #getPageLinkList(boolean)} returns the links as a Java <code>ArrayList\<String\></code>.
     * 
     * @param bExtract if true, the links are extracted and cached from the page source before returned. If false, the cached links will be returned only
     * @return String with the current page links, stripped of markup tags
     */
    // TODO: add defitition of links
    public String getPageLinks(boolean bExtract) {
        if (bExtract)
            extractPageLinks();

        String pageLinks = new String();

        if (_currentPageLinks.size() > 0) {
            for (int i = 0; i < _currentPageLinks.size(); ++i)
                pageLinks = pageLinks.concat(_currentPageLinks.get(i).replaceAll("%s", _linkSeparator).concat(_linkListSeparator));
            pageLinks = pageLinks.substring(0, pageLinks.length() - 1);
        }

        return pageLinks;
    }

    /**
     * Private method to do the actual data extraction from the page source. Caches the result in a private variable.
     */
    private void extractPageLinks() {
        _currentPageLinks.clear();

        // extract the text body of the article
        int iStartIndex = _currentPageSource.indexOf("<text ");
        int iEndIndex = _currentPageSource.indexOf("</text>");
        if (iStartIndex < 0 || iEndIndex < 0)
            return;

        String body = _currentPageSource.substring(iStartIndex, iEndIndex);
        String[] links = body.split("\\[\\[");

        boolean bFirstLoop = true;
        for (String link : links) {
            // the first string cannot contain a link
            if (bFirstLoop == true) {
                bFirstLoop = false;
                continue;
            }

            // this skips links like [[File: description]]
            if (link.contains(":"))
                continue;

            // if there were opening brackets, but no closing ones, ignore
            iEndIndex = link.indexOf("]]");
            if (iEndIndex < 0)
                continue;

            // cut text behind the closing brackets
            link = link.substring(0, iEndIndex);

            // clean special links
            if (link.contains("#"))
                link = link.substring(0, link.indexOf("#"));
            else if (link.contains("|"))
                link = link.substring(0, link.indexOf("|"));

            // clean html tags
            if (link.contains("&lt;")) {
                link = link.replaceAll("&lt;", "<<<<<");
                link = link.replaceAll("&gt;", ">>>>>");
                link = link.replaceAll("<<<<<[^>>>>>]*>>>>>", " ");
            }

            // clean quotation marks
            if (link.contains("&quot")) {
                iStartIndex = link.indexOf("&quot;");
                iEndIndex = link.indexOf("&quot;", iStartIndex);
                link = link.substring(iStartIndex, iEndIndex + 6);
            }
            // if the current links is already in the list, don't add a second time
            if (_currentPageLinks.contains(link))
                continue;

            _currentPageLinks.add(link);
        }
    }

    /**
     * Convenience wrapper for {@link #getPageType(boolean)}, always passing true so that the extraction process is initiated.  This is the function to be used
     * normally (when the information is requested the first (or the only) time).
     * 
     * @return enum a value from {@link PageType} designating the page type
     */
    public PageType getPageType() {
        return getPageType(true);
    }

    /**
     * Returns a value from {@link PageType} designating the page type. With the Boolean argument set to true, it will extract the information from the curren
     * tpage source. If false is passed in, the cached value will be returned only
     * 
     * @return enum value designating the page type as specified in {@link PageType}
     */
    public PageType getPageType(boolean bExtract) {
        if (bExtract)
            extractPageType();

        return _currentPageType;
    }

    /**
     * Private method to parse the page source and determine the page type. Caches the result in a private variable.
     */
    private void extractPageType() {
        _currentPageType = PageType.UNKNOWN;

        // assume that a page can only be valid if it has text and a title
        if (!_currentPageSource.isEmpty() && !getPageTitle(true).isEmpty()) {
            if (_currentPageSource.contains("<redirect "))
                _currentPageType = PageType.REDIRECT;

            else if (_currentPageTitle.startsWith("Wikipedia:"))
                _currentPageType = PageType.WIKIPEDIA;

            else if (_currentPageTitle.startsWith("File:"))
                _currentPageType = PageType.FILE;

            else if (_currentPageTitle.startsWith("Template:"))
                _currentPageType = PageType.TEMPLATE;

            else if (_currentPageTitle.startsWith("Category:"))
                _currentPageType = PageType.CATEGORY;

            else if (_currentPageTitle.startsWith("Portal:"))
                _currentPageType = PageType.PORTAL;
            else if (_currentPageTitle.startsWith("Media:")
            		|| _currentPageTitle.startsWith("User:")
            		|| _currentPageTitle.startsWith("MediaWiki:")
            		|| _currentPageTitle.startsWith("Help:")
            		|| _currentPageTitle.startsWith("Book:")
            		|| _currentPageTitle.startsWith("Education Program:")
            		|| _currentPageTitle.startsWith("TimedText:")
            		|| _currentPageTitle.startsWith("Module:")
            		
            		// discard articles in month_year (e.g. January 2002) format
            		|| _currentPageTitle.matches("^(?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{4}$")
            		
            		// discard articles in year_in… (e.g. 2002 in literature, 1996 in the Olympics) format
            		|| _currentPageTitle.matches("^\\d{4} in .*")
            		
            		// discard articles in only digit format (e.g. 1996, 819382, 42)
            		|| _currentPageTitle.matches("^\\d+$")
            		
            		// discard articles in list format (e.g. List of ... )
            		|| _currentPageTitle.startsWith("List of")
            		)
            {
                _currentPageType = PageType.UNKNOWN;
            }

            else if (_currentPageTitle.endsWith("(disambiguation)") || _currentPageSource.contains("{{disambig}}") || _currentPageSource.contains("{{Disambig}}"))
                _currentPageType = PageType.DISAMBIGUATION;

            else
                _currentPageType = PageType.ARTICLE;
        }
    }

    /**
     * Private method to do the actual data extraction from the XML file. Caches the result in a private variable.
     */
    private void extractPageSource() {
        _currentPageSource = "";

        try {
            // advance to first line that contains the <page> tag (or end of file)
            String line = _dumpfileReader.readLine();
            if (line == null)
                throw (new Exception());

            while (!line.contains("<page>")) {
                line = _dumpfileReader.readLine();
                if (line == null)
                    throw (new Exception());
            }

            line = _dumpfileReader.readLine();
            if (line == null)
                throw (new Exception());

            // as long as we don't arrive at the </page> tag, keep adding lines
            _currentPageSource = "";
            while (!line.contains("</page>")) {
                _currentPageSource += line;
                line = _dumpfileReader.readLine();
            }
        } catch (Exception e) {
            _currentPageSource = "";
        }
    }

    /**
     * With <code>setTitleSeparator</code> the String that separates words within a title is set. This is by default "_" so the title can be used as a unique
     * identifier for an article. It may however sometimes be useful to use another character like a whitespace.
     * 
     * @param a <code>String</code> containing the title separator
     */
    public void setTitleSeparator(String titleSeparator) {
        _titleSeparator = titleSeparator;
    }

    /**
     * Returns the string that is used to separate words within a title.
     * 
     * @return a <code>String</code> containing the title separator
     */
    public String getTitleSeparator() {
        return _titleSeparator;
    }

    /**
     * With <code>setCategorySeparator</code> the String that separates words within a category is set. This is by default "_" so the category can be used as a
     * unique identifier for a category. It may however sometimes be useful to use another character like a whitespace.
     * 
     * @param a <code>String</code> containing the category separator
     */
    public void setCategorySeparator(String categorySeparator) {
        _categorySeparator = categorySeparator;
    }

    /**
     * Returns the string that is used to separate words within a category
     * 
     * @return a <code>String</code> containing the title separator
     */
    public String getCategorySeparator() {
        return _categorySeparator;
    }

    /**
     * With <code>setCategoryListSeparator</code> the String that separates categories from each other (when the list of categories is returned as a single
     * string) is set. This is by default a whitespace. The String should be chosen with care as to not cause unwanted effects with the way that the words
     * inside a category string are separated.
     * 
     * @param a <code>String</code> containing the string that separates categories from each other when returned as a single string
     */
    public void setCategoryListSeparator(String categoryListSeparator) {
        _categoryListSeparator = categoryListSeparator;
    }

    /**
     * Returns the string that is used to separate categories from each other when returned as a single string
     * 
     * @return a <code>String</code> used to separate categories from each other when returned as a single string
     */
    public String getCategoryListSeparator() {
        return _categoryListSeparator;
    }

    /**
     * With <code>setLinkSeparator</code> the String that separates words within a link is set. This is by default "_" so the link can be used as a unique
     * identifier for a link. It may however sometimes be useful to use another character like a whitespace.
     * 
     * @param a <code>String</code> containing the link separator
     */
    public void setLinkSeparator(String linkSeparator) {
        _linkSeparator = linkSeparator;
    }

    /**
     * Returns the string that is used to separate words within a link
     * 
     * @return a <code>String</code> containing the title separator
     */
    public String getLinkSeparator() {
        return _linkSeparator;
    }

    /**
     * With <code>setLinkListSeparator</code> the String that separates links from each other (when the list of links is returned as a single string) is
     * set. This is by default a whitespace. The String should be chosen with care as to not cause unwanted effects with the way that the words inside a link
     * string are separated.
     * 
     * @param a <code>String</code> containing the string that separates links from each other when returned as a single string
     */
    public void setLinkListSeparator(String linkListSeparator) {
        _linkListSeparator = linkListSeparator;
    }

    /**
     * Returns the string that is used to separate links from each other when returned as a single string
     * 
     * @return a <code>String</code> used to separate links from each other when returned as a single string
     */
    public String getLinkListSeparator() {
        return _linkListSeparator;
    }
    
    public static final String[] stopCategories = new String[] {
    "Star name disambiguations"
    ,"America"
    ,"Disambiguation"
    ,"Georgia"
    ,"Lists of political parties by generic name"
    ,"Galaxy name disambiguations"
    ,"Lists of two-letter combinations"
    ,"Disambiguation categories"
    ,"Towns in Italy (disambiguation)"
    ,"Redirects to disambiguation pages"
    ,"Birmingham"
    ,"Mathematical disambiguation"
    ,"Public schools in Montgomery County"
    ,"Structured lists"
    ,"Identical titles for unrelated songs"
    ,"Signpost articles"
    ,"Township disambiguation"
    ,"County disambiguation"
    ,"Disambiguation pages in need of cleanup"
    ,"Human name disambiguation"
    ,"Number disambiguations"
    ,"Letter and number combinations"
    ,"4-letter acronyms"
    ,"Acronyms that may need to be disambiguated"
    ,"Lists of roads sharing the same title"
    ,"List disambiguations"
    ,"3-digit Interstate disambiguations"
    ,"Geographical locations sharing the same title"
    ,"Tropical cyclone disambiguation"
    ,"Repeat-word disambiguations"
    ,"Song disambiguations"
    ,"Disambiguated phrases"
    ,"Subway station disambiguations"
    ,"Lists of identical but unrelated album titles"
    ,"5-letter acronyms"
    ,"Three-letter acronym disambiguations"
    ,"Miscellaneous disambiguations"
    ,"Two-letter acronym disambiguations"
    ,"Days"
    ,"Eastern Orthodox liturgical days"
    };
    public static final Set<String> stopCategoriesSet = new HashSet<String>(Arrays.asList(stopCategories));
    public static final String[] stopWords = new String[] { 
       "able","about","above","according","accordingly","across","actually","after","afterwards","again"
		,"against","ain","albeit","all","allow","allows","almost","alone","along","already"
		,"also","although","always","among","amongst","and","another","any","anybody","anyhow"
		,"anyone","anything","anyway","anyways","anywhere","apart","appear","appreciate","appropriate","are"
		,"aren","around","aside","ask","asking","associated","available","away","awfully","became"
		,"because","become","becomes","becoming","been","before","beforehand","behind","being","believe"
		,"below","beside","besides","best","better","between","beyond","both","brief","but"
		,"mon","came","can","can","cannot","cant","cause","causes","certain","certainly"
		,"changes","clearly","com","come","comes","concerning","consequently","consider","considering","contain"
		,"containing","contains","corresponding","could","couldn","course","currently","definitely","described","despite"
		,"did","didn","different","does","doesn","doing","don","done","down","downwards"
		,"during","each","edu","eight","either","else","elsewhere","enough","entirely","especially"
		,"etc","even","ever","every","everybody","everyone","everything","everywhere","exactly","example"
		,"except","far","few","fifth","first","five","followed","following","follows","for"
		,"former","formerly","forth","four","from","further","furthermore","get","gets","getting"
		,"given","gives","goes","going","gone","got","gotten","greetings","had","hadn"
		,"happens","hardly","has","hasn","have","haven","having","hello","help","hence"
		,"her","here","here","hereafter","hereby","herein","hereupon","hers","herself","him"
		,"himself","his","hither","hopefully","how","howbeit","however","ignored","immediate","inasmuch"
		,"inc","indeed","indicate","indicated","indicates","inner","insofar","instead","into","inward"
		,"isn","its","itself","just","keep","keeps","kept","know","known","knows"
		,"last","lately","later","latter","latterly","least","less","lest","let","like"
		,"liked","likely","little","look","looking","looks","ltd","mainly","many","may"
		,"maybe","mean","meanwhile","merely","might","more","moreover","most","mostly","much"
		,"must","myself","name","namely","near","nearly","necessary","need","needs","neither"
		,"never","nevertheless","new","next","nine","nobody","non","none","noone","nor"
		,"normally","not","nothing","novel","now","nowhere","obviously","off","often","okay"
		,"old","once","one","ones","only","onto","other","others","otherwise","ought"
		,"our","ours","ourselves","out","outside","over","overall","own","particular","particularly"
		,"per","perhaps","placed","please","plus","possible","presumably","probably","provides","que"
		,"quite","rather","really","reasonably","regarding","regardless","regards","relatively","respectively","right"
		,"said","same","saw","say","saying","says","second","secondly","see","seeing"
		,"seem","seemed","seeming","seems","seen","self","selves","sensible","sent","serious"
		,"seriously","seven","several","shall","she","should","shouldn","since","six","some"
		,"somebody","somehow","someone","something","sometime","sometimes","somewhat","somewhere","soon","sorry"
		,"specified","specify","specifying","still","sub","such","sup","sure","take","taken"
		,"tell","tends","than","thank","thanks","thanx","that","that","thats","the"
		,"their","theirs","them","themselves","then","thence","there","there","thereafter","thereby"
		,"therefore","therein","theres","thereupon","these","they","think","third","this","thorough"
		,"thoroughly","those","though","three","through","throughout","thru","thus","together","too"
		,"took","toward","towards","tried","tries","truly","try","trying","twice","two"
		,"under","unfortunately","unless","unlikely","until","unto","upon","use","used","useful"
		,"uses","using","usually","uucp","value","various","very","via","viz","want"
		,"wants","was","wasn","way","welcome","well","went","were","weren","what"
		,"whatever","whatsoever","when","whence","whenever","whensoever","where","where","whereafter","whereas"
		,"whereat","whereby","wherefrom","wherein","whereinto","whereof","whereon","whereto","whereunto","whereupon"
		,"wherever","wherewith","whether","which","whichever","whichsoever","while","whilst","whither","who"
		,"whoever","whole","whom","whomever","whomsoever","whose","whosoever","why","will","willing"
		,"wish","with","within","without","won","wonder","would","wouldn","yes","yet"
		,"you","your","yours","yourself","yourselves","zero", "ref"
	};
    public static final Set<String> stopWordSet = new HashSet<String>(Arrays.asList(stopWords));
}
