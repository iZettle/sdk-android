GET HELP

Wordfence Intelligence Documentation
How can we help you today?
Search Help
  
Suggestions:
← Return to Wordfence Intelligence Documentation Index
V2: Accessing and Consuming the Vulnerability Data Feed
Vulnerability Data Feed
The Vulnerability Data Feed provides the most-current information about vulnerabilities impacting WordPress. This feed requires no authentication, and is publicly available for free for personal and commercial use. By using the Vulnerability Data Feed API you acknowledge that you have read and agree to the Wordfence Intelligence Terms and Conditions.

If you have any questions on how to get started, please reach out to us at wfi-support@wordfence.com.

Accessing the Vulnerability Data Feed
Two versions of the Vulnerability Data Feed are available to support different use cases:

Production Feed – Detailed records that have been fully analyzed by the Wordfence team
Scanner Feed – Minimal format that provides detection information for newly discovered vulnerabilities that are actively being researched in addition to those included in the Production Feed
Both corresponding endpoints return the complete feed and do not accept any additional parameters.

Production Feed
The Production Feed provides data about vulnerabilities for which complete details are available. This may not include all records that are available in the Scanner Feed.

GET /api/intelligence/v2/vulnerabilities/production
Host: www.wordfence.com
Example Record
(See Data Format for details)

{
	"848ccbdc-c6f1-480f-a272-cd459e706713": {
        "id": "848ccbdc-c6f1-480f-a272-cd459e706713",
        "title": "Example Vulnerability",
        "software": [
            {
                "type": "plugin",
                "name": "Example Plugin",
                "slug": "example",
                "affected_versions": {
                    "1.0.0 - 1.2.3": {
                        "from_version": "1.0.0",
                        "from_inclusive": true,
                        "to_version": "1.2.3",
                        "to_inclusive": true
                    }
                },
                "patched": true,
                "patched_versions": [
                    "1.2.4"
                ],
                "remediation": "Update to version 1.2.4, or a newer patched version"
            }
        ],
        "informational": false,
        "description": "An example vulnerability",
        "references": [
            "http:\/\/www.wordfence.com/threat-intel/vulnerabilities/example"
        ],
        "cwe": {
            "id": 80,
            "name": "Improper Neutralization of Script-Related HTML Tags in a Web Page (Basic XSS)",
            "description": "The software receives input from an upstream component, but it does not neutralize or incorrectly neutralizes special characters such as <, >, and & that could be interpreted as web-scripting elements when they are sent to a downstream component that processes web pages."
        },
        "cvss": {
            "vector": "CVSS:3.1\/A:N\/I:L\/C:L\/S:U\/UI:N\/PR:N\/AC:L\/AV:N",
            "score": 6.5,
            "rating": "Medium"
        },
        "cve": "CVE-1998-1000",
        "cve_link": "https:\/\/www.cve.org\/CVERecord?id=CVE-1998-1000",
        "researchers": [
            "A. Researcher"
        ],
        "published": "1998-01-09 00:00:00",
        "updated": "2022-08-05 20:14:05",
        "copyrights": {
            "message": "This record contains material that is subject to copyright",
            "defiant": {
                "notice": "Copyright 2012-2023 Defiant Inc.",
                "license": "Defiant hereby grants you a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, sublicense, and distribute this software vulnerability information. Any copy of the software vulnerability information you make for such purposes is authorized provided that you, include a hyperlink to this vulnerability record, and reproduce Defiant's copyright designation and this license in any such copy.",
                "license_url": "https:\/\/www.wordfence.com\/wti-community-edition-terms-and-conditions\/"
            },
            "mitre": {
                "notice": "Copyright 1999-2022 The MITRE Corporation",
                "license": "CVE Usage: MITRE hereby grants you a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, sublicense, and distribute Common Vulnerabilities and Exposures (CVE\u00ae). Any copy you make for such purposes is authorized provided that you reproduce MITRE's copyright designation and this license in any such copy.",
                "license_url": "https:\/\/www.cve.org\/Legal\/TermsOfUse"
             }
        }
    },
    ...
}
Scanner Feed
The Scanner Feed contains only detection information and includes new vulnerabilities that do not yet have enough information to be included in the Production Feed.

GET /api/intelligence/v2/vulnerabilities/scanner
Host: www.wordfence.com
Example Record
(See Data Format for details)

{
    "848ccbdc-c6f1-480f-a272-cd459e706713": {
        "id": "848ccbdc-c6f1-480f-a272-cd459e706713",
        "title": "Example Vulnerability",
        "software": [
            {
                "type": "plugin",
                "name": "Example Plugin",
                "slug": "example",
                "affected_versions": {
                    "1.0.0 - 1.2.3": {
                        "from_version": "1.0.0",
                        "from_inclusive": true,
                        "to_version": "1.2.3",
                        "to_inclusive": true
                    }
                },
                "patched": true,
                "patched_versions": [
                    "1.2.4"
                ],
        "informational": false,
        "references": [
            "http:\/\/www.wordfence.com/threat-intel/vulnerabilities/example"
        ],
            }
        ],
        "published": "1998-01-09 00:00:00"
        "copyrights": {
            "message": "This record contains material that is subject to copyright",
            "defiant": {
                "notice": "Copyright 2012-2023 Defiant Inc.",
                "license": "Defiant hereby grants you a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, sublicense, and distribute this software vulnerability information. Any copy of the software vulnerability information you make for such purposes is authorized provided that you, include a hyperlink to this vulnerability record, and reproduce Defiant's copyright designation and this license in any such copy.",
                "license_url": "https:\/\/www.wordfence.com\/wti-community-edition-terms-and-conditions\/"
            }
    },
    ...
}
Data Format
Both versions of the Vulnerability Data Feed are provided as JSON and follow the same basic format, though the Production Feed contains additional fields.

The root element in the JSON is an object where the keys are UUIDs assigned by Wordfence to each vulnerability. This allows flexibility in parsing the feed; it can easily be loaded into either a map (keyed by UUID) or just a sequential array, depending on how the data is to be used. The value is an object containing details about the corresponding vulnerability.

Record Fields
Field	Type	Description	Feed
id	string	A UUID string that serves as a unique identifier for the vulnerability	All
title	string	A human-readable title for the vulnerability	All
software	array	A list of affected software (see Software Format)	All
informational	boolean	Whether or not this vulnerability is considered informational. Informational vulnerabilities have extremely limited or no real-world impact.	All
description	string	A human-readable description of the vulnerability	Production
references	array	An array of URL’s that are relevant to the vulnerability	All
cwe	object or null	CWE details when available, null otherwise (see CWE Format)	Production
cvss	object or null	CVSS details when available, null otherwise (see CVSS Format)	Production
cve	string or null	The CVE ID (i.e. CVE-1998-1000)when assigned, null otherwise	Production
cve_link	string or null	The URL to a page providing details on the assigned CVE, when applicable; null otherwise	Production
researchers	array	The names (one string per researcher) of individuals who researched this vulnerability	Production
published	string or null	The date at which the vulnerability was publically disclosed or null if it has not yet been disclosed (see Date Format)	All
updated	string or null	The date at which the vulnerability was last updated (see Date Format)	Production
copyrights	object or null	Copyright details for the vulnerability or null if no copyright is applicable (see Copyright Format)	All
Software Format
An array of affected software is specified for each vulnerability across all feeds. Each value in this array is an object with the following fields describing the affected software and version information:

Field	Type	Description	Feed
type	string	core, plugin, or theme	All
name	string	A human-readable name for the software	All
slug	string	An identifier for the software	All
affected_versions	object	A set of affected versions (see Affected Version Format)	All
patched	boolean	Whether or not the software has been patched for the relevant vulnerability	All
patched_versions	array	A list of version numbers that include patches for the relevant vulnerability	All
remediation	string	A string describing the recommended remediation for the relevant vulnerability	Production
Affected Version Format
Affected version information is provided as an object with a string representation of the version as a key. This string may be a single version, an inclusive range, or an interval (specified using parentheses or square brackets to notate open or closed intervals, respectively). The value of an object with the following fields representing a range of versions:

Field	Type	Description	Feed
from_version	string	The lowest version of the range (or the special value *)	All
from_inclusive	boolean	Whether or not the from_version is included in the range	All
to_version	string	The highest version of the range (or the special value *)	All
to_inclusive	boolean	Whether or not the to_version is included in the range	All
Note that an asterisk (*) is a special value that denotes any version. It is only meaningful when it is the only character in the string and it cannot be combined with other versions values (i.e. 1.* would match the asterisk literally rather than matching 1.0). It may appear in either the from_version or to_version fields.

CWE Format
The Common Weakness Enumeration (CWE) defines standardized identifiers for common types of vulnerabilities.

Field	Type	Description	Feed
id	integer	The ID of the relevant CWE	Production
name	string	The name of the relevant CWE	Production
description	string	A description of the relevant CWE	Production
CVSS Format
The Common Vulnerability Scoring System (CVSS) provides a framework for rating the severity of vulnerabilities. The Vulnerability Data Feed may include both CVSS v3.0 and CVSS v3.1 formatted records.

Field	Type	Description	Feed
vector	string	The CVSS vector string (i.e. CVSS:3.1/A:N/I:L/C:L/S:U/UI:N/PR:N/AC:L/AV:N)	Production
score	number	A numeric score between 0.0 and 10.0 representing the severity of the vulnerability, with higher values being more severe	Production
rating	string	A textual rating of the severity (None, Low, Medium, High, or Critical)	Production
Date Format
Date/time values are provided as strings in the following format: YYYY-MM-DD hh:mm:ss

YYYY – 4 digit year
MM – 2 digit year with leading zeroes
DD – 2 digit month with leading zeros
hh – 2 digit hour with leading zeroes
mm – 2 digit minute with leading zeroes
ss – 2 digit second with leading zeroes
All time values are UTC unless otherwise specified.

Copyright Format
When any party has copyright claims to vulnerability data, a copyrights field will be included with details about the relevant party and its claims.

Field	Type	Description	Feed
message	string	A message about the copyright status of the vulnerability	All
notice	string	A copyright notice in a standard format	All
license	string	The text of the license terms under which the copyrighted material is to be used	All
license_url	string	A URL at which the relevant license can be found	All
MITRE Attribution Requirement
Any company or individual who uses our vulnerability database API needs to display the MITRE copyright claims included in that vulnerability record for any MITRE vulnerabilities that they display to their end user.

Wordfence Intelligence Documentation
Changelog
V2: Authenticating to the Wordfence Intelligence API (Enterprise)
V2: Accessing and Consuming the Vulnerability Data Feed
V2: Accessing and Querying the Malware Hash Feed (Enterprise)
V2: Accessing and Using the Malware Signature Feed (Enterprise)
V2: Querying the Wordfence Intelligence IP Data API (Enterprise)
Querying the Wordfence Intelligence IP Data API (Enterprise)
Accessing and Consuming the Vulnerability Data Feed
Accessing and Using the Malware Detection Feed (Enterprise)
Authenticating to the Wordfence Intelligence API (Enterprise)
Our business hours are 9am-8pm ET, 6am-5pm PT and 2pm-1am UTC/GMT excluding weekends and holidays.
Response customers receive 24-hour support, 365 days a year, with a 1-hour response time.
Terms of Service
Privacy Policy and Notice at Collection
Products

Wordfence Free
Wordfence Premium
Wordfence Care
Wordfence Response
Wordfence CLI
Wordfence Intelligence
Wordfence Intelligence Enterprise
Wordfence Central
Support

Documentation
Learning Center
Free Support
Premium Support
News

Blog
In The News
Vulnerability Advisories
About

About Wordfence
Careers
Contact
Security
CVE Request Form
Stay Updated

Sign up for news and updates from our panel of experienced security professionals.

rizqshops@googlegroups.com
By checking this box I agree to the terms of service and privacy policy.*
ISO 27001 Certified by Intercert
© 2012-2023 Defiant Inc. All Rights Reserve
