# FiLiPo
FiLiPo (Finding Linkge Points) is a data integration tool. It is used to align the schemes of a local Knowledge Base and a Web API. 

## How to use the Programme
To be done..

## Aligned Datasets
| Local Knowledge Base  | Aligned Web API                                                                           |
| --------------------- |-------------------------------------------------------------------------------------------|
| dblp                  | CrossRef, SciGraph, Semantic Scholar (DOI), Semantic Scholar (ArXiv-Key), ArXiv, Elsevier |
| Linked Movie DB       | Open Movie Database                                                                       |

## Usability
The average values were calculated by performing three test series. In all test series the precision and recall were determined. Then the average for this table was determined. The first table shows our results when using the regular expression approach.

| Aligning (RegExer)                    | Sample<br>Size | Mean<br>Precision | Mean<br>Recall | F1 Score |
| --------------------------------------|----------------|-------------------|----------------|----------|
| dblp - CrossRef (DOI)                 | 100            | 0.91              | 0.79           | 0.84     |
| dblp - SciGraph (DOI)                 | 100            | 0.85              | 0.76           | 0.80     |
| dblp - Semantic Scholar (DOI)         | 100            | 0.93              | 1.00           | 0.96     |
| dblp - Semantic Scholar (ArXiv-Key)   | 100            | 1.00              | 1.00           | 1.00     |
| dblp - Arxiv (ArXiv-Key)              | 100            | 1.00              | 1.00           | 1.00     |
| dblp - elsevier (DOI)                 | 400            | 0.97              | 0.69           | 0.81     |

The second table shows our results when using the gradient boosting classifier instead of the regular expression approach.

| Aligning (GB Classifier)              | Sample<br>Size | Mean<br>Precision | Mean<br>Recall | F1 Score |
| --------------------------------------|----------------|-------------------|----------------|----------|
| dblp - CrossRef                       | 100            | 0.99              | 0.76           | 0.86     |
| dblp - SciGraph                       | 100            | 0.94              | 0.67           | 0.78     |
| dblp - Semantic Scholar (DOI)         | 100            | 0.92              | 1.00           | 0.96     |
| dblp - Semantic Scholar (ArXiv-Key)   | 100            | 1.00              | 1.00           | 1.00     |
| dblp - Arxiv                          | 100            | 0.89              | 1.00           | 0.94     |
| dblp - elsevierDOI                    | 400            | 0.96              | 0.72           | 0.83     |

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
## References
1. String-Similarity by Baltes et. al, [GitHub](https://github.com/sotorrent/string-similarity), [![DOI](https://zenodo.org/badge/98212408.svg)](https://zenodo.org/badge/latestdoi/98212408)
2. dblp
3. Linked Movie DB
4. CrossRef 
5. SciGraph
6. Semantic Scholar (DOI)
7. Semantic Scholar (ArXiv-Key)
8. Open Movie Database
