# FiLiPo
FiLiPo is a system designed to simplify data integration. To do this, it determines a mapping between the schema of a local knowledge base and that of an API. This mapping specifies how the data can be integrated into the local Knowledge Base. The goal of FiLiPo was to enable non-technical users (e.g. data curators) to use this system. For this reason, only a few parameters need to be specified.

## Publications
* Tobias Zeimetz, Ralf Schenkel<br/>
  [Sample Driven Data Mapping for Linked Data and Web APIs](https://www.uni-trier.de/fileadmin/fb4/prof/INF/DBI/Publikationen/submitted_cikm2020_zeimetz_schenkel.pdf)<br/>
  In CIKM Demo Track 2020
  * Demo Video: [Link](https://basilika.uni-trier.de/nextcloud/s/vN3Za1gpHmOAEuR)
  * CIKM Source Code: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.4010483.svg)](https://doi.org/10.5281/zenodo.4010483)

## Aligned Datasets
| Local Knowledge Base  | Aligned Web API                                                                                             |
| --------------------------------- |-------------------------------------------------------------------------------------------------------------|
| dblp[[2](#references)]            | CrossRef[[4](#references)], SciGraph[[5](#references)], Semantic Scholar (DOI)[[6](#references)], Semantic Scholar (ArXiv-Key)[[6](#references)], Open Citations[[10](#references)]ArXiv[[7](#references)], Elsevier[[8](#references)] |
| Linked Movie DB[[3](#references)] | Open Movie Database API[[9](#references)], The Movie Database[[11](#references)]  
| IMDB[[12](#references)]           | Open Movie Database API[[9](#references)]|

## Usability
We have evaluated precision and recall of FiLiPo on several knowledge bases and Web APIs. The average values for precision and recall were calculated by performing mutliple test series. The runtime of FiLiPo was between 15-45 Minutes, depending on the sample size and the response time of the Web API. For the evaluation we used the metrics precision, recall and F1 Score. FiLiPo was able to achieve a precision between 0.73 to 1.00 and a recall between 0.66 to 1.00. Values close to 1.0 were achieved mainly because there were only a few possible alignments. The corresponding F1 scores for FiLiPo are between 0.69 and 0.95.

## Usable Similarity Methods
We used the string similarity framework by Baltes et. al [[1](#references)]. The table below lists all string similarity methods that can be used. Note, that for `n` you can use the values `n=2,3,4,5`. 

| Category      | Methods                                                           |
| ------------- |-------------------------------------------------------------------|
| Equal         | Equal, Equal Normalized, Tokken Equal, Token Equal Normalized     |
| Edit-based    | Levenshtein, Levenshtein Normalized,<br>Damerau-Levenshtein, Damerau-Levenshtein Normalized,<br>Optimal-Alignment, Optimal-Alignment Normalized,<br>Longest-Common-Subsequence, Longest-Common-Subsequence Normalized |
| Set-based     | Jaccard Token, Jaccard Token Normalized,<br>Sorensen-Dice Toke, Sorensen-Dice Token Normalized,<br>Overlap Token\*, Overlap Token Normalized\*<br>Jaccard n-grams, Jaccard n-grams Normalized, Jaccard n-grams Normalized Padding,<br>Sorensen-Dice n-grams, Sorensen-Dice n-grams Normalized, Sorensen-Dice n-grams Normalized Padding,<br>Overlap n-grams\*, Overlap n-grams Normalized\*, Overlap n-grams Normalized Padding\*,<br>Jaccard n-shingles, Jaccard n-shingles Normalized,<br> Sorensen-Dice n-shingles, Sorensen-Dice n-shingles Normalized,<br>Overlap n-shingles, Overlap n-shingles Normalized           |

\* We do not recommend using these methods as they may lead to inaccurate results. Only experts should use them.

## Configuration File
This section gives a brief overview of the configurations that can be done by an expert user. First the global settings will be explained. They are used to control the output of the programme, specify the level of detail in the log file and so on. Afterwards the aligning settings will be described, which can be used by an technical user to fine-tune the system. 

### Globals
```
{
  "globals": {
    "logpath":"res/log/",             // Path to log files 
    "outpath": "res/",                // Path of programme output
    "dbpath": "database.json",        // Path to locales 
    "scpath": "supconf.json",
    "secretpath": "secrets.json",     // Path to Web API secrets
    "ipc": "tcp://*:5555",            // Address to communicate with
                                      // the gradient boosting classifier                  
    "timeout": "500",                 // Waiting time after an API request 
                                      // in order to prevent flooding
    "mode": "0",                      // 0 non-technical user, 1 technical user
                                      // 2 evaluation, 3 demo
    "loglevel": "0"                   // Range of 1-4
  } 
  ...
```

### Aligning Settings
```
  "linkage_config": {
    "similarity_requests": "100",         // Requests send to a Web API (sample size)

    "candidate_requests": "25",           // Probing size (number of initial requests)
    "string_similarity": "0.5",           // How similar two string need to be in order
                                          // to yield as equals (e.g. two titles)
    "record_similarity": "0.1",           // Overlapping between data records in order 
                                          // to yield as valid response
    "distribution_variance": "0.4",       // 
    "candidate_responses": "0.1",         //
    "error_threshold": "0.8",             //
    "traversal_depth": "2",               // 
    "functionality_threshold": "0.99",    // Every relation that has a functionality
                                          // greater than 0.99 is consideres as identifier
    "classifier": "regex",                // Used to specify if the regular expression
                                          // approach (regex) will be used or the gradient
                                          // boosting classifier (gbc)

    "support_mode": "0",
    "min_support_match": "0.5",
    "min_support_nonmatch": "0.1",

    "similarity_metrics": [
      ...
    ]
  },
```

### Configuring RegExer
This section of the `config.json` file is used to add rules (similar to an regular expression) to the `RegExer` class.

```
  "ruleset":[
    {
      "name": "isbn-issn",
      "filter": "-"                   // The user can specify in the filter field which
                                      // characters will be ignored when comparing values
    },
    {
      "name": "insensitive-uri",
      "filter": "/i"                  // The flag /i is used to specify that cases will be
                                      // ignored when comparing values
    },
    {
      "name": "fuzzy",
      "filter": "/f"                  // Use the best matching similarity method
                                      // (this can lead to errorenous results)
    }
  ]
```

## Citing 
If you find FiLiPo useful in your research, please consider citing the following paper:
```bibtex
@inproceedings{filipo,
  author = {Zeimetz, Tobias and Schenkel, Ralf},
  title = {Sample Driven Data Mapping for Linked Data and Web APIs},
  year = {2020},
  url = {https://doi.org/10.1145/3340531.3417438},
  doi = {10.1145/3340531.3417438},
  booktitle = {Proceedings of the 29th ACM International Conference on Information &amp; Knowledge Management},
  pages = {3481–3484}
}
```

## Updates
* Version 1.2: Minor bug fixes and added an output file that is used by an Angular GUI to represent the results in an easy understandable way. The corresponding GUI will be published with the next update.
* Version 1.1: Added a functionality to determine joint features. This feature is used to find out which commonalities entities had that led to a response from the API. For example, you can find out that an API only responds to articles from a specific publisher.

## References
1. String-Similarity by Baltes et. al, [GitHub](https://github.com/sotorrent/string-similarity)
2. [dblp](https://dblp.uni-trier.de/)
3. [Linked Movie DB](http://www.cs.toronto.edu/~oktie/linkedmdb/linkedmdb-18-05-2009-dump.nt)
4. [CrossRef API](https://www.crossref.org/services/metadata-delivery/rest-api/)
5. [SciGraph API](https://scigraph.springernature.com/explorer/api/)
6. [Semantic Scholar](https://api.semanticscholar.org)
7. [Arxiv API](https://arxiv.org/help/api)
8. [Elsevier API](https://api.elsevier.com)
9. [Open Movie Database API](http://www.omdbapi.com)
10. [Open Citations](https://opencitations.net/index/coci/api/v1)
11. [The Movie Database](https://developers.themoviedb.org/3/find/find-by-id)
12. IMDB in RDF Format
