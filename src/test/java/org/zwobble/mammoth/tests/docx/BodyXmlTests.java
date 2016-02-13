package org.zwobble.mammoth.tests.docx;

import com.natpryce.makeiteasy.Maker;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.zwobble.mammoth.documents.*;
import org.zwobble.mammoth.docx.*;
import org.zwobble.mammoth.results.Result;
import org.zwobble.mammoth.results.Warning;
import org.zwobble.mammoth.tests.DeepReflectionMatcher;
import org.zwobble.mammoth.xml.XmlElement;
import org.zwobble.mammoth.xml.XmlNode;
import org.zwobble.mammoth.xml.XmlNodes;

import java.util.List;
import java.util.Optional;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.zwobble.mammoth.documents.NoteReference.endnoteReference;
import static org.zwobble.mammoth.documents.NoteReference.footnoteReference;
import static org.zwobble.mammoth.results.Warning.warning;
import static org.zwobble.mammoth.tests.DeepReflectionMatcher.deepEquals;
import static org.zwobble.mammoth.tests.documents.DocumentElementMakers.*;
import static org.zwobble.mammoth.tests.docx.BodyXmlReaderMakers.*;
import static org.zwobble.mammoth.util.MammothLists.list;
import static org.zwobble.mammoth.util.MammothMaps.map;
import static org.zwobble.mammoth.xml.XmlNodes.element;

public class BodyXmlTests {

    @Test
    public void textFromTextElementIsRead() {
        XmlElement element = textXml("Hello!");
        assertThat(readSuccess(a(bodyReader), element), isTextElement("Hello!"));
    }

    @Test
    public void canReadTextWithinRun() {
        XmlElement element = runXml(list(textXml("Hello!")));
        assertThat(
            readSuccess(a(bodyReader), element),
            isRun(run(text("Hello!"))));
    }

    @Test
    public void canReadTextWithinParagraph() {
        XmlElement element = paragraphXml(list(runXml(list(textXml("Hello!")))));
        assertThat(
            readSuccess(a(bodyReader), element),
            isParagraph(make(a(PARAGRAPH, with(CHILDREN, list(run(text("Hello!"))))))));
    }

    @Test
    public void paragraphHasNoStyleIfItHasNoProperties() {
        XmlElement element = paragraphXml();
        assertThat(
            readSuccess(a(bodyReader), element),
            hasStyle(Optional.empty()));
    }

    @Test
    public void whenParagraphHasStyleIdInStylesThenStyleNameIsReadFromStyles() {
        XmlElement element = paragraphXml(list(
            element("w:pPr", list(
                element("w:pStyle", map("w:val", "Heading1"))))));

        Style style = new Style("Heading1", Optional.of("Heading 1"));
        Styles styles = new Styles(
            map("Heading1", style),
            map());
        assertThat(
            readSuccess(a(bodyReader, with(STYLES, styles)), element),
            hasStyle(Optional.of(style)));
    }

    @Test
    public void warningIsEmittedWhenParagraphStyleCannotBeFound() {
        XmlElement element = paragraphXml(list(
            element("w:pPr", list(
                element("w:pStyle", map("w:val", "Heading1"))))));
        assertThat(
            read(a(bodyReader), element),
            isResult(
                hasStyle(Optional.of(new Style("Heading1", Optional.empty()))),
                list(warning("Paragraph style with ID Heading1 was referenced but not defined in the document"))));
    }

    @Test
    public void paragraphHasNoNumberingIfItHasNoNumberingProperties() {
        XmlElement element = paragraphXml();
        assertThat(
            readSuccess(a(bodyReader), element),
            hasNumbering(Optional.empty()));
    }

    @Test
    public void paragraphHasNumberingPropertiesFromParagraphPropertiesIfPresent() {
        XmlElement element = paragraphXml(list(
            element("w:pPr", list(
                element("w:numPr", map(), list(
                    element("w:ilvl", map("w:val", "1")),
                    element("w:numId", map("w:val", "42"))))))));

        Numbering numbering = new Numbering(map("42", map("1", NumberingLevel.ordered("1"))));

        assertThat(
            readSuccess(a(bodyReader, with(NUMBERING, numbering)), element),
            hasNumbering(NumberingLevel.ordered("1")));
    }

    @Test
    public void numberingPropertiesAreIgnoredIfLevelIsMissing() {
        // TODO: emit warning
        XmlElement element = paragraphXml(list(
            element("w:pPr", list(
                element("w:numPr", map(), list(
                    element("w:numId", map("w:val", "42"))))))));

        Numbering numbering = new Numbering(map("42", map("1", NumberingLevel.ordered("1"))));

        assertThat(
            readSuccess(a(bodyReader, with(NUMBERING, numbering)), element),
            hasNumbering(Optional.empty()));
    }

    @Test
    public void numberingPropertiesAreIgnoredIfNumIdIsMissing() {
        // TODO: emit warning
        XmlElement element = paragraphXml(list(
            element("w:pPr", list(
                element("w:numPr", map(), list(
                    element("w:ilvl", map("w:val", "1"))))))));

        Numbering numbering = new Numbering(map("42", map("1", NumberingLevel.ordered("1"))));

        assertThat(
            readSuccess(a(bodyReader, with(NUMBERING, numbering)), element),
            hasNumbering(Optional.empty()));
    }

    @Test
    public void runHasNoStyleIfItHasNoProperties() {
        XmlElement element = runXml(list());
        assertThat(
            readSuccess(a(bodyReader), element),
            hasStyle(Optional.empty()));
    }

    @Test
    public void whenRunHasStyleIdInStylesThenStyleNameIsReadFromStyles() {
        XmlElement element = runXml(list(
            element("w:rPr", list(
                element("w:rStyle", map("w:val", "Heading1Char"))))));

        Style style = new Style("Heading1Char", Optional.of("Heading 1 Char"));
        Styles styles = new Styles(
            map(),
            map("Heading1Char", style));
        assertThat(
            readSuccess(a(bodyReader, with(STYLES, styles)), element),
            hasStyle(Optional.of(style)));
    }

    @Test
    public void warningIsEmittedWhenRunStyleCannotBeFound() {
        XmlElement element = runXml(list(
            element("w:rPr", list(
                element("w:rStyle", map("w:val", "Heading1Char"))))));

        assertThat(
            read(a(bodyReader), element),
            isResult(
                hasStyle(Optional.of(new Style("Heading1Char", Optional.empty()))),
                list(warning("Run style with ID Heading1Char was referenced but not defined in the document"))));
    }

    @Test
    public void runIsNotBoldIfBoldElementIsNotPresent() {
        XmlElement element = runXmlWithProperties();

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("bold", equalTo(false)));
    }

    @Test
    public void runIsBoldIfBoldElementIsPresent() {
        XmlElement element = runXmlWithProperties(element("w:b"));

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("bold", equalTo(true)));
    }

    @Test
    public void runIsNotItalicIfItalicElementIsNotPresent() {
        XmlElement element = runXmlWithProperties();

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("italic", equalTo(false)));
    }

    @Test
    public void runIsItalicIfItalicElementIsPresent() {
        XmlElement element = runXmlWithProperties(element("w:i"));

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("italic", equalTo(true)));
    }

    @Test
    public void runIsNotUnderlinedIfUnderlineElementIsNotPresent() {
        XmlElement element = runXmlWithProperties();

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("underline", equalTo(false)));
    }

    @Test
    public void runIsUnderlinedIfUnderlineElementIsPresent() {
        XmlElement element = runXmlWithProperties(element("w:u"));

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("underline", equalTo(true)));
    }

    @Test
    public void runIsNotStruckthroughIfStrikethroughElementIsNotPresent() {
        XmlElement element = runXmlWithProperties();

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("strikethrough", equalTo(false)));
    }

    @Test
    public void runIsStruckthroughIfStrikethroughElementIsPresent() {
        XmlElement element = runXmlWithProperties(element("w:strike"));

        assertThat(
            readSuccess(a(bodyReader), element),
            hasProperty("strikethrough", equalTo(true)));
    }

    // TODO: vertical alignment

    @Test
    public void canReadTabElement() {
        XmlElement element = element("w:tab");

        assertThat(
            readSuccess(a(bodyReader), element),
            equalTo(Tab.TAB));
    }

    @Test
    public void brIsReadAsLineBreak() {
        XmlElement element = element("w:br");

        assertThat(
            readSuccess(a(bodyReader), element),
            equalTo(LineBreak.LINE_BREAK));
    }

    @Test
    public void warningOnBreaksThatArentLineBreaks() {
        XmlElement element = element("w:br", map("w:type", "page"));

        assertThat(
            readAll(a(bodyReader), element),
            isResult(equalTo(list()), list(warning("Unsupported break type: page"))));
    }

    @Test
    public void canReadTableElements() {
        XmlElement element = element("w:tbl", list(
            element("w:tr", list(
                element("w:tc", list(
                    element("w:p")))))));

        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(new Table(list(
                new TableRow(list(
                    new TableCell(list(
                        make(a(PARAGRAPH))))))))));
    }

    @Test
    public void hyperlinkIsReadIfItHasARelationshipId() {
        Relationships relationships = new Relationships(
            map("r42", new Relationship("http://example.com")));
        XmlElement element = element("w:hyperlink", map("r:id", "r42"), list(runXml(list())));
        assertThat(
            readSuccess(a(bodyReader, with(RELATIONSHIPS, relationships)), element),
            deepEquals(Hyperlink.href("http://example.com", list(make(a(RUN))))));
    }

    @Test
    public void hyperlinkIsReadIfItHasAnAnchorAttribute() {
        XmlElement element = element("w:hyperlink", map("w:anchor", "start"), list(runXml(list())));
        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(Hyperlink.anchor("start", list(make(a(RUN))))));
    }

    @Test
    public void hyperlinkIsIgnoredIfItDoesNotHaveARelationshipIdNorAnchor() {
        XmlElement element = element("w:hyperlink", list(runXml(list())));
        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(make(a(RUN))));
    }

    @Test
    public void goBackBookmarkIsIgnored() {
        XmlElement element = element("w:bookmarkStart", map("w:name", "_GoBack"));
        assertThat(
            readAll(a(bodyReader), element),
            isResult(equalTo(list()), list()));
    }

    @Test
    public void bookmarkStartIsReadIfNameIsNotGoBack() {
        XmlElement element = element("w:bookmarkStart", map("w:name", "start"));
        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(new Bookmark("start")));
    }

    @Test
    public void footnoteReferenceHasIdRead() {
        XmlElement element = element("w:footnoteReference", map("w:id", "4"));
        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(footnoteReference("4")));
    }

    @Test
    public void endnoteReferenceHasIdRead() {
        XmlElement element = element("w:endnoteReference", map("w:id", "4"));
        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(endnoteReference("4")));
    }

    @Test
    public void textBoxesHaveContentAppendedAfterContainingParagraph() {
        XmlElement textBox = element("w:pict", list(
            element("v:shape", list(
                element("v:textbox", list(
                    element("w:txbxContent", list(
                        paragraphXml(list(
                            runXml(list(textXml("[textbox-content]")))))))))))));
        XmlElement paragraph = paragraphXml(list(
            runXml(list(textXml("[paragragh start]"))),
            runXml(list(textBox, textXml("[paragragh end]")))));

        List<DocumentElement> expected = list(
            make(a(PARAGRAPH, with(CHILDREN, list(
                make(a(RUN, with(CHILDREN, list(
                    new Text("[paragragh start]"))))),
                make(a(RUN, with(CHILDREN, list(
                    new Text("[paragragh end]"))))))))),
            make(a(PARAGRAPH, with(CHILDREN, list(
                make(a(RUN, with(CHILDREN, list(
                    new Text("[textbox-content]"))))))))));

        assertThat(
            readAll(a(bodyReader), paragraph),
            isResult(deepEquals(expected), list()));
    }

    @Test
    public void appropriateElementsHaveTheirChildrenReadNormally() {
        assertChildrenAreReadNormally("w:ins");
        assertChildrenAreReadNormally("w:smartTag");
        assertChildrenAreReadNormally("w:drawing");
        assertChildrenAreReadNormally("v:roundrect");
        assertChildrenAreReadNormally("v:shape");
        assertChildrenAreReadNormally("v:textbox");
        assertChildrenAreReadNormally("w:txbxContent");
    }

    private void assertChildrenAreReadNormally(String name) {
        XmlElement element = element(name, list(paragraphXml()));

        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(make(a(PARAGRAPH))));
    }

    @Test
    public void ignoredElementsAreIgnoredWithoutWarning() {
        assertIsIgnored("office-word:wrap");
        assertIsIgnored("v:shadow");
        assertIsIgnored("v:shapetype");
        assertIsIgnored("w:bookmarkEnd");
        assertIsIgnored("w:sectPr");
        assertIsIgnored("w:proofErr");
        assertIsIgnored("w:lastRenderedPageBreak");
        assertIsIgnored("w:commentRangeStart");
        assertIsIgnored("w:commentRangeEnd");
        assertIsIgnored("w:commentReference");
        assertIsIgnored("w:del");
        assertIsIgnored("w:footnoteRef");
        assertIsIgnored("w:endnoteRef");
        assertIsIgnored("w:pPr");
        assertIsIgnored("w:rPr");
        assertIsIgnored("w:tblPr");
        assertIsIgnored("w:tblGrid");
        assertIsIgnored("w:tcPr");
    }

    private void assertIsIgnored(String name) {
        XmlElement element = element(name, list(paragraphXml()));

        assertThat(
            readAll(a(bodyReader), element),
            isResult(equalTo(list()), list()));
    }

    @Test
    public void unrecognisedElementsAreIgnoredWithWarning() {
        XmlElement element = element("w:huh");
        assertThat(
            readAll(a(bodyReader), element),
            isResult(equalTo(list()), list(warning("An unrecognised element was ignored: w:huh"))));
    }

    @Test
    public void textNodesAreIgnoredWhenReadingChildren() {
        XmlElement element = runXml(list(XmlNodes.text("[text]")));
        assertThat(
            readSuccess(a(bodyReader), element),
            deepEquals(make(a(RUN))));
    }

    private static DocumentElement readSuccess(Maker<BodyXmlReader> reader, XmlElement element) {
        Result<DocumentElement> result = read(reader, element);
        assertThat(result.getWarnings(), deepEquals(list()));
        return result.getValue();
    }

    private static Result<DocumentElement> read(Maker<BodyXmlReader> reader, XmlElement element) {
        Result<List<DocumentElement>> result = readAll(reader, element);
        assertThat(result.getValue(), Matchers.hasSize(1));
        return result.map(elements -> elements.get(0));
    }

    private static Result<List<DocumentElement>> readAll(Maker<BodyXmlReader> reader, XmlElement element) {
        return reader.make().readElement(element).toResult();
    }

    private XmlElement paragraphXml() {
        return paragraphXml(list());
    }

    private XmlElement paragraphXml(List<XmlNode> children) {
        return element("w:p", children);
    }

    private XmlElement runXml(List<XmlNode> children) {
        return element("w:r", children);
    }

    private XmlElement runXmlWithProperties(XmlNode... children) {
        return element("w:r", list(element("w:rPr", asList(children))));
    }

    private XmlElement textXml(String value) {
        return element("w:t", list(XmlNodes.text(value)));
    }

    private Matcher<DocumentElement> isParagraph(Paragraph expected) {
        return new DeepReflectionMatcher<>(expected);
    }

    private Matcher<DocumentElement> isRun(Run expected) {
        return new DeepReflectionMatcher<>(expected);
    }

    private Matcher<DocumentElement> isTextElement(String value) {
        return new DeepReflectionMatcher<>(new Text(value));
    }

    private Matcher<? super DocumentElement> hasStyle(Optional<Style> expected) {
        return hasProperty("style", deepEquals(expected));
    }

    private Matcher<? super DocumentElement> hasNumbering(NumberingLevel expected) {
        return hasNumbering(Optional.of(expected));
    }

    private Matcher<? super DocumentElement> hasNumbering(Optional<NumberingLevel> expected) {
        return hasProperty("numbering", deepEquals(expected));
    }

    private <T> Matcher<Result<? extends T>> isResult(Matcher<T> valueMatcher, List<Warning> warnings) {
        return Matchers.allOf(
            hasProperty("value", valueMatcher),
            hasProperty("warnings", deepEquals(warnings)));
    }

    private Run run(DocumentElement... children) {
        return make(a(RUN, with(CHILDREN, asList(children))));
    }

    private Text text(String value) {
        return new Text(value);
    }
}
