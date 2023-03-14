# Session 06 worklog

## 1) Add Monitoring to Your Systems
TODO

## 2) Software Maintenance
At the current time, there are no issuse present in our system based on the dashboard provided by Helge and the TA's.

Group a, dashboard: http://104.248.134.203/status.html


## 3) Software Maintenance II
We group a Academic Weapons -[checks]-> Group b DevUps: Delivering Buggy Software Late since 2023.

We've created 3 issues on group B's github, concernning the issues we found while testing their Minitwit. A big issue is, no twits are shown, when we access the site. It appears the website is loading all twits, and therefore works until the browser eventually suggests closing the webpage, or the webpage itself just freeze and completely ceases all functionality. 

It's possible to sign-up, however, on success you're redirected to a 404 page. You can return to the main page, and press login before the frontpage freezes, and you'll actually be logged in. This will show a box wherein you're able to write a twit, with other twits possibly being shown below. However, this also struggles with the same issue as first mentioned, and it will eventually freeze / never show any twits.

Lastly &mdash; a bit nickpicky &mdash; but attempting to sign-up with a taken username, results in a nice pop-up that simply displays the message "an error occurred", and it's also possible to sign up with an invalid email address, ie. test@t.

Functionally concerning other users and twits weren't possible to test, as no twits &mdash; and therefore users &mdash; where never shown. 