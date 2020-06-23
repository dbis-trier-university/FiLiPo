# FiLiPo
FiLiPo (Finding Linkge Points) is a data integration tool. It is used to align the schemes of a local Knowledge Base and a Web API. 

## How to use the Programme

## Usable Similarity Methods

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
