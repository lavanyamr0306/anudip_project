from __future__ import annotations

import datetime as dt
import textwrap
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

OUT_DIR = Path(__file__).resolve().parent / "presentation"
OUT_FILE = OUT_DIR / "Resume_Analyzer_Pro_Modern_Presentation.pptx"

SLIDE_W = 9144000
SLIDE_H = 6858000

NAVY = "0B1020"
INDIGO = "182848"
ACCENT = "64D2FF"
ACCENT_2 = "A78BFA"
WHITE = "F8FAFC"
MUTED = "CBD5E1"


def esc(text: str) -> str:
    return escape(text, {"'": "&apos;", '"': "&quot;"})


def run(text: str, size: int, color: str, bold: bool = False) -> str:
    b = ' b="1"' if bold else ""
    return (
        f'<a:r><a:rPr lang="en-US" sz="{size}"{b}><a:solidFill><a:srgbClr val="{color}"/></a:solidFill></a:rPr>'
        f"<a:t>{esc(text)}</a:t></a:r>"
    )


def para(text: str, size: int = 2200, color: str = WHITE, bold: bool = False) -> str:
    return f'<a:p>{run(text, size, color, bold)}<a:endParaRPr lang="en-US" sz="{size}"/></a:p>'


def shape_rect(shape_id: int, name: str, x: int, y: int, w: int, h: int, fill: str, line: str | None = None) -> str:
    line_xml = (
        f'<a:ln w="12700"><a:solidFill><a:srgbClr val="{line}"/></a:solidFill></a:ln>'
        if line
        else "<a:ln><a:noFill/></a:ln>"
    )
    return f"""<p:sp>
<p:nvSpPr><p:cNvPr id="{shape_id}" name="{esc(name)}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
<p:spPr><a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val="{fill}"/></a:solidFill>{line_xml}</p:spPr>
<p:txBody><a:bodyPr/><a:lstStyle/><a:p/></p:txBody>
</p:sp>"""


def text_box(shape_id: int, name: str, x: int, y: int, w: int, h: int, paragraphs: list[str]) -> str:
    return f"""<p:sp>
<p:nvSpPr><p:cNvPr id="{shape_id}" name="{esc(name)}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
<p:spPr><a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:noFill/><a:ln><a:noFill/></a:ln></p:spPr>
<p:txBody><a:bodyPr wrap="square" anchor="t"/><a:lstStyle/>{''.join(paragraphs)}</p:txBody>
</p:sp>"""


def wrap_lines(items: list[str], width: int) -> list[str]:
    out: list[str] = []
    for item in items:
        out.extend(textwrap.wrap(item, width=width, break_long_words=False, break_on_hyphens=False) or [""])
    return out


def standard_slide(title: str, bullets: list[str], idx: int) -> str:
    title_paras = [para(title, 2800, WHITE, True)]
    body_paras = [para(f"• {line}", 2000, MUTED) for line in wrap_lines(bullets, 68)]
    parts = [
        shape_rect(1, f"Background {idx}", 0, 0, SLIDE_W, SLIDE_H, NAVY),
        shape_rect(2, f"Top Band {idx}", 0, 0, SLIDE_W, 720000, INDIGO),
        shape_rect(3, f"Accent Line {idx}", 540000, 1040000, 1500000, 50000, ACCENT),
        text_box(4, f"Title {idx}", 540000, 300000, 7800000, 600000, title_paras),
        text_box(5, f"Body {idx}", 740000, 1350000, 7600000, 4700000, body_paras),
        shape_rect(6, f"Accent Block {idx}", 8120000, 620000, 450000, 450000, ACCENT_2),
    ]
    return slide_shell(parts)


def divider_slide(title: str, subtitle: str, idx: int) -> str:
    parts = [
        shape_rect(1, f"Background {idx}", 0, 0, SLIDE_W, SLIDE_H, INDIGO),
        shape_rect(2, f"Accent Bar {idx}", 1200000, 2500000, 6744000, 80000, ACCENT),
        text_box(3, f"Title {idx}", 1200000, 1800000, 6744000, 700000, [para(title, 3600, WHITE, True)]),
        text_box(4, f"Subtitle {idx}", 1200000, 2850000, 6744000, 500000, [para(subtitle, 2200, MUTED)]),
    ]
    return slide_shell(parts)


def title_slide(title: str, subtitle: str, meta: list[str], idx: int) -> str:
    meta_paras = [para(line, 1900, MUTED, False) for line in meta]
    parts = [
        shape_rect(1, f"Background {idx}", 0, 0, SLIDE_W, SLIDE_H, NAVY),
        shape_rect(2, f"Glow Left {idx}", 300000, 800000, 2200000, 2200000, INDIGO),
        shape_rect(3, f"Accent Bar {idx}", 760000, 1680000, 1000000, 70000, ACCENT),
        text_box(4, f"Main Title {idx}", 760000, 1100000, 7000000, 900000, [para(title, 3600, WHITE, True)]),
        text_box(5, f"Subtitle {idx}", 760000, 1900000, 7000000, 500000, [para(subtitle, 2200, ACCENT)]),
        text_box(6, f"Meta {idx}", 760000, 2800000, 7000000, 2200000, meta_paras),
        shape_rect(7, f"Accent Block {idx}", 7920000, 1050000, 600000, 600000, ACCENT_2),
    ]
    return slide_shell(parts)


def slide_shell(parts: list[str]) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="100" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>{''.join(parts)}</p:spTree></p:cSld>
<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>"""


def slide_rel() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/></Relationships>"""


def layout_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="titleAndText" preserve="1"><p:cSld name="Title and Content"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>"""


def layout_rel() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/></Relationships>"""


def master_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld name="Modern Dark"><p:bg><p:bgRef idx="1001"><a:schemeClr val="bg1"/></p:bgRef></p:bg><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/><p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst><p:txStyles><p:titleStyle><a:lvl1pPr algn="l"><a:defRPr sz="3200" b="1"/></a:lvl1pPr></p:titleStyle><p:bodyStyle><a:lvl1pPr marL="0" indent="0"><a:defRPr sz="2000"/></a:lvl1pPr></p:bodyStyle><p:otherStyle><a:defPPr><a:defRPr sz="1800"/></a:defPPr></p:otherStyle></p:txStyles></p:sldMaster>"""


def master_rel() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/></Relationships>"""


def theme_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Resume Analyzer Modern Theme"><a:themeElements><a:clrScheme name="Dark Resume"><a:dk1><a:srgbClr val="0B1020"/></a:dk1><a:lt1><a:srgbClr val="F8FAFC"/></a:lt1><a:dk2><a:srgbClr val="182848"/></a:dk2><a:lt2><a:srgbClr val="CBD5E1"/></a:lt2><a:accent1><a:srgbClr val="64D2FF"/></a:accent1><a:accent2><a:srgbClr val="A78BFA"/></a:accent2><a:accent3><a:srgbClr val="38BDF8"/></a:accent3><a:accent4><a:srgbClr val="22C55E"/></a:accent4><a:accent5><a:srgbClr val="F59E0B"/></a:accent5><a:accent6><a:srgbClr val="EF4444"/></a:accent6><a:hlink><a:srgbClr val="64D2FF"/></a:hlink><a:folHlink><a:srgbClr val="A78BFA"/></a:folHlink></a:clrScheme><a:fontScheme name="Modern"><a:majorFont><a:latin typeface="Segoe UI"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont><a:minorFont><a:latin typeface="Calibri"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont></a:fontScheme><a:fmtScheme name="Modern"><a:fillStyleLst><a:solidFill><a:schemeClr val="lt1"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="9525"><a:solidFill><a:schemeClr val="accent1"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="dk1"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements><a:objectDefaults/><a:extraClrSchemeLst/></a:theme>"""


def pres_xml(count: int) -> str:
    ids = "".join(f'<p:sldId id="{255+i}" r:id="rId{i+1}"/>' for i in range(1, count + 1))
    return f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst><p:sldIdLst>{ids}</p:sldIdLst><p:sldSz cx="{SLIDE_W}" cy="{SLIDE_H}" type="screen4x3"/><p:notesSz cx="6858000" cy="9144000"/></p:presentation>'


def pres_rel(count: int) -> str:
    rels = ['<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>']
    rels += [f'<Relationship Id="rId{i+1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>' for i in range(1, count + 1)]
    return f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">{"".join(rels)}</Relationships>'


def types_xml(count: int) -> str:
    slides = "".join(f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>' for i in range(1, count + 1))
    return f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/><Override PartName="/ppt/presProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presProps+xml"/><Override PartName="/ppt/viewProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.viewProps+xml"/><Override PartName="/ppt/tableStyles.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.tableStyles+xml"/><Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/><Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/><Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>{slides}</Types>'


def root_rel() -> str:
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>'


def app_xml(count: int) -> str:
    return f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Microsoft Office PowerPoint</Application><PresentationFormat>On-screen Show (4:3)</PresentationFormat><Slides>{count}</Slides><Notes>0</Notes><HiddenSlides>0</HiddenSlides><MMClips>0</MMClips><ScaleCrop>false</ScaleCrop><HeadingPairs><vt:vector size="2" baseType="variant"><vt:variant><vt:lpstr>Theme</vt:lpstr></vt:variant><vt:variant><vt:i4>1</vt:i4></vt:variant></vt:vector></HeadingPairs><TitlesOfParts><vt:vector size="1" baseType="lpstr"><vt:lpstr>Resume Analyzer Pro Modern Presentation</vt:lpstr></vt:vector></TitlesOfParts><Company/><LinksUpToDate>false</LinksUpToDate><SharedDoc>false</SharedDoc><HyperlinksChanged>false</HyperlinksChanged><AppVersion>16.0000</AppVersion></Properties>'


def core_xml() -> str:
    now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    return f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>Resume Analyzer Pro Modern Presentation</dc:title><dc:subject>Modern major project PPT</dc:subject><dc:creator>Codex</dc:creator><cp:lastModifiedBy>Codex</cp:lastModifiedBy><dcterms:created xsi:type="dcterms:W3CDTF">{now}</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">{now}</dcterms:modified></cp:coreProperties>'


def pres_props() -> str:
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentationPr xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>'


def view_props() -> str:
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:viewPr xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:normalViewPr/><p:slideViewPr><p:cSldViewPr snapToGrid="1" snapToObjects="1"/></p:slideViewPr><p:notesTextViewPr/><p:gridSpacing cx="72008" cy="72008"/></p:viewPr>'


def table_styles() -> str:
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><a:tblStyleLst xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" def="{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}"/>'


def slides_data() -> list[tuple[str, str, list[str]]]:
    d = dt.date(2026, 4, 19).strftime("%d %B %Y")
    return [
        ("title", "Resume Analyzer Pro", ["Automated Resume Analyzer with Job Fit Scoring System", "Presented by: Lavanya M R", "Roll Number: [Add Roll Number]", "Department: [Add Department]", "College: Sai Vidya Institute of Technology", "Guide: [Add Guide Name]", f"Date: {d}"]),
        ("divider", "Introduction", ["Project Overview and Need"]),
        ("standard", "Introduction", ["Resume Analyzer Pro is a web-based application for analyzing resumes against job descriptions.", "It measures resume-job fit using keyword match, tracked skill match, and completeness score.", "The system identifies matched skills, missing skills, and recommended job roles.", "It is useful for students, job seekers, and placement support teams."]),
        ("standard", "Problem Statement", ["Candidates often do not know whether their resumes match a target role before applying.", "Manual resume review is slow, repetitive, and inconsistent.", "Existing tools often provide generic feedback or unclear scoring.", "Users need fast, clear, and actionable resume analysis."]),
        ("standard", "Objectives", ["Build a web application for resume analysis against a job description.", "Support both file upload and pasted resume text.", "Generate keyword match, skill match, completeness, and final score.", "Recommend suitable job roles based on analysis results.", "Provide history tracking, PDF export, and admin monitoring."]),
        ("standard", "Scope of the Project", ["Covers registration, login, resume submission, score generation, result storage, and PDF reports.", "Includes an admin module for user review, locking, unlocking, and deletion.", "Supports rule-based recommendation using a technical skills inventory.", "Does not include semantic AI matching, external job portals, or production-scale cloud deployment."]),
        ("standard", "Literature Review", ["Spring Boot documentation supports rapid enterprise Java application development.", "Thymeleaf supports server-side HTML rendering for web applications.", "Apache Tika enables text extraction from PDF and document files.", "Many ATS systems focus mostly on keyword filtering with limited transparency.", "This project combines extraction, scoring, storage, and administration in one platform."]),
        ("standard", "Existing System", ["Resume checking is usually manual or spread across disconnected tools.", "Many systems provide only generic tips or a single opaque score.", "Previous approaches rarely maintain structured analysis history.", "Administrative monitoring is often absent in small academic tools."]),
        ("standard", "Proposed System", ["A centralized web application automates resume screening and job-fit scoring.", "The system accepts uploaded resumes or pasted text and compares them with a job description.", "It displays final score, matched skills, missing skills, and recommended roles.", "It also stores history, exports PDF reports, and supports admin review."]),
        ("divider", "Architecture", ["System Design and Workflow"]),
        ("standard", "System Architecture", ["User and admin interact through Thymeleaf-based pages.", "Controllers route requests to service classes.", "Service layer handles parsing, scoring, recommendation, and report generation.", "Repositories persist users, resumes, and analyses into MySQL.", "Apache Tika extracts content from uploaded files before analysis."]),
        ("standard", "Architecture Components", ["AuthController manages registration, login, logout, and role-based redirects.", "ResumeController handles analysis flow, history loading, and PDF download.", "AdminController manages user records and analysis review.", "ResumeService and UserService implement business logic and validation.", "JPA repositories connect the service layer to the database."]),
        ("standard", "Methodology", ["Collect resume input from uploaded file or pasted text.", "Extract content using Apache Tika when a file is uploaded.", "Normalize resume text and job description.", "Calculate keyword match, tracked skills score, and completeness score.", "Generate final score, derive status, save analysis, and display the result."]),
        ("standard", "Technologies Used", ["Java for core application development.", "Spring Boot 3.2.0 for backend framework and MVC.", "Thymeleaf, HTML, and CSS for frontend rendering.", "MySQL with Spring Data JPA and Hibernate for persistence.", "Apache Tika for parsing documents and iText for PDF generation.", "Maven, Git, and GitHub for build and version control."]),
        ("standard", "Database Design", ["User entity stores name, email, password, role, and locked status.", "Resume entity stores file name, extracted content, score, and linked user.", "Analysis entity stores final score, score breakdown, status, insight, job description, and timestamp.", "Additional tables store matched skills, missing skills, and recommended jobs.", "The design supports one-to-many relationships between user, resumes, and analyses."]),
        ("standard", "Implementation", ["Built authentication for both users and admins.", "Added resume submission using file upload and direct text input.", "Implemented score calculation, skill matching, and role recommendation logic.", "Stored analysis history with timestamps and user mapping.", "Generated downloadable PDF reports and admin monitoring pages."]),
        ("divider", "Features", ["User and Admin Modules"]),
        ("standard", "User Features", ["Register and log in to the application.", "Upload a resume or paste resume text.", "Enter job description and receive score-based analysis.", "View matched skills, missing skills, and recommended job roles.", "Download analysis as PDF and revisit saved history."]),
        ("standard", "Admin Features", ["View all users and all generated analyses.", "Inspect a selected user's resumes and analysis history.", "Open detailed analysis pages for review.", "Lock or unlock accounts to control access.", "Delete a user and related resume or analysis records."]),
        ("standard", "Results / Output", ["Dashboard displays final score, keyword score, skill score, and completeness score.", "The system highlights matched skills, missing skills, and recommended roles.", "Users can compare progress through stored analysis history.", "PDF reports provide portable analysis summaries.", "Admin dashboard provides supervision and user-level visibility."]),
        ("standard", "Advantages", ["Fast and easy resume evaluation.", "Clear and transparent score breakdown.", "Supports both uploaded files and direct text input.", "Stores history for repeated improvement.", "Includes admin control and PDF export."]),
        ("standard", "Limitations", ["Current matching logic is mainly keyword-based.", "It does not fully capture semantic meaning of resume content.", "Password security can be improved for production use.", "The recommendation engine depends on predefined skills and rules.", "There is no direct integration with external job platforms."]),
        ("standard", "Future Scope", ["Add AI-based semantic analysis for stronger matching.", "Improve security with password hashing and stronger authorization.", "Integrate external job portal APIs.", "Add resume rewriting suggestions and richer analytics.", "Support cloud deployment and advanced recruiter workflows."]),
        ("standard", "Conclusion", ["Resume Analyzer Pro solves a practical resume-job fit problem through a full-stack web platform.", "The project combines parsing, scoring, persistence, reporting, and administration in one system.", "It provides meaningful feedback for resume improvement and academic demonstration.", "The current system is a strong foundation for future enhancement."]),
        ("standard", "References", ["Frameworks: Spring Boot Documentation, Thymeleaf Documentation, Spring Data JPA Documentation.", "Tools: Maven Documentation, MySQL Documentation, GitHub project repository.", "Libraries: Apache Tika Documentation, iText PDF Documentation."]),
        ("divider", "Thank You", ["Any Questions?"]),
    ]


def build() -> Path:
    slides = slides_data()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUT_FILE, "w", compression=zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", types_xml(len(slides)))
        z.writestr("_rels/.rels", root_rel())
        z.writestr("docProps/app.xml", app_xml(len(slides)))
        z.writestr("docProps/core.xml", core_xml())
        z.writestr("ppt/presentation.xml", pres_xml(len(slides)))
        z.writestr("ppt/_rels/presentation.xml.rels", pres_rel(len(slides)))
        z.writestr("ppt/presProps.xml", pres_props())
        z.writestr("ppt/viewProps.xml", view_props())
        z.writestr("ppt/tableStyles.xml", table_styles())
        z.writestr("ppt/theme/theme1.xml", theme_xml())
        z.writestr("ppt/slideMasters/slideMaster1.xml", master_xml())
        z.writestr("ppt/slideMasters/_rels/slideMaster1.xml.rels", master_rel())
        z.writestr("ppt/slideLayouts/slideLayout1.xml", layout_xml())
        z.writestr("ppt/slideLayouts/_rels/slideLayout1.xml.rels", layout_rel())
        for i, (kind, title, lines) in enumerate(slides, start=1):
            if kind == "title":
                xml = title_slide(title, lines[0], lines[1:], i)
            elif kind == "divider":
                xml = divider_slide(title, lines[0], i)
            else:
                xml = standard_slide(title, lines, i)
            z.writestr(f"ppt/slides/slide{i}.xml", xml)
            z.writestr(f"ppt/slides/_rels/slide{i}.xml.rels", slide_rel())
    return OUT_FILE


if __name__ == "__main__":
    print(build())
