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
    "similarity_requests": "25",

    "candidate_requests": "5",
    "string_similarity": "0.5",
    "record_similarity": "0.1",
    "distribution_variance": "0.4",
    "candidate_responses": "0.1",
    "error_threshold": "0.8",
    "traversal_depth": "2",
    "functionality_threshold": "0.996",
    "classifier": "regex",

    "support_mode": "0",
    "min_support_match": "0.5",
    "min_support_nonmatch": "0.1",

    "similarity_metrics": [
      ...
    ]
  },
```

### Configuring RegExer
```
  "ruleset":[
    {
      "name": "isbn-issn",
      "filter": "-"
    },
    {
      "name": "insensitive-uri",
      "filter": "/i"
    },
    {
      "name": "fuzzy",
      "filter": "/f"
    }
  ]
```
