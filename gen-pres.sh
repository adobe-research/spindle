#!/bin/bash

cat>pres.html<<EOF
<!DOCTYPE html>
<!--
///////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
///////////////////////////////////////////////////////////////////////////
-->
<html lang="en"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta charset="utf-8">
  <title>Spindle</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="description" content="">
  <meta name="author" content="">

  <link href="css/bootstrap.css" rel="stylesheet">
  <style type="text/css">
body {
  padding-top: 60px;
  padding-bottom: 40px;
}
  </style>
  <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
  <!--[if lt IE 9]>
  <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
  <![endif]-->

<style type="text/css"></style></head>

<body>
  <div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
    <div class="container">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
          <span class="sr-only">Toggle navigation</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand" href="http://adobe-research.github.io/spindle">Spindle</a>
      </div>
      <div class="collapse navbar-collapse">
        <ul class="nav navbar-nav">
          <li><a href="http://adobe-research.github.io/spindle">Home</a></li>
          <li><a href="http://adobe-research.github.io/spindle/adhoc">Ad hoc</a></li>
          <li class="active"><a href="http://adobe-research.github.io/spindle/pres">Presentation</a></li>
          <li><a href="http://github.com/adobe-research/spindle">GitHub</a></li>
        </ul>
      </div>
    </div>
  </div>

  <script src="./index_files/jquery-latest.js"></script>


  <div class="container">
   <div class="hero-unit">
     <center>
     <a href="/slides/cloudcom-pres-full.pdf">Full Presentation</a> |
     <a href="/slides/cloudcom-pres-handout.pdf">Handout</a>
     </center>

     Spindle will be presented at
     <a href="http://2014.cloudcom.org">CloudCom 2014</a>.
     The LaTeX source of these slides is available from
     <a href="https://github.com/adobe-research/spindle/tree/master/cloudcom-2014-presentation">here</a> in the main Spindle repository and uses code portions
     from <a href="https://github.com/bamos/beamer-snippets">bamos/beamer-snippets</a>.
     <center>
EOF

cd slides
rm *.png
gs -dBATCH -dNOPAUSE -sDEVICE=png16m -r350 \
   -sOutputFile=slide-%03d.png cloudcom-pres-handout.pdf
./gen-html.py >> ../pres.html
cd ..

cat>>pres.html<<EOF
      </center>
    </div>
  </div>
  <script src="./index_files/bootstrap.js"></script>
</body></html>
EOF
