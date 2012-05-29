log = LOAD '$input' USING PigStorage(',')
    AS (action:chararray, uid:chararray, uidint:int, oid:chararray, oidint:int, 
        source:chararray, sourceint:int,
        length_title:int, length_desc:int, length_content:int,
        occur_time:long);
data = FILTER log BY action != 'null' AND uid != '-1' AND oid != 'null'
        AND source != 'null' AND length_title >= 0 AND length_desc >= 0
        AND length_content >= 0 AND occur_time > 0;

--get max value of features
data_grouped = GROUP data ALL;
max_feas = FOREACH data_grouped GENERATE MAX(data.uidint) AS max_uidint,
            MAX(data.sourceint) AS max_sourceint, 
            MAX(data.length_title) AS max_length_title,
            MAX(data.length_desc) AS max_length_desc,
            MAX(data.length_content) AS max_length_content;

records = FOREACH data GENERATE CONCAT(uid, oid) AS id, 
        action, uidint, oidint, sourceint, length_title, 
        length_desc, length_content, occur_time;
--TODO:here extract features
features = 
    FOREACH records GENERATE id, action, uidint, oidint, sourceint, 
        length_title, length_desc, length_content;
features_no_action = 
    FOREACH records GENERATE id, uidint, oidint, sourceint, 
        length_title, length_desc, length_content;
features_distinct = DISTINCT features_no_action;

show_feas = FILTER features BY action=='show';
click_feas = FILTER features BY action=='click';
/*unclick_feas = FILTER features BY action=='unclick';*/
/*like_feas = FILTER features BY action=='like';*/

g_show_feas = GROUP show_feas BY id;
g_click_feas = GROUP click_feas BY id;
/*g_unclick_feas = GROUP unclick_feas BY id;*/
/*g_like_feas = GROUP like_feas BY id;*/

show = FOREACH g_show_feas GENERATE group AS gid, COUNT(show_feas) AS show_num;
click = FOREACH g_click_feas GENERATE group AS gid, COUNT(click_feas) AS click_num;
/*unclick = FOREACH g_unclick_feas GENERATE group AS gid, COUNT(unclick_feas) AS unclick_num;*/
/*like = FOREACH g_like_feas GENERATE group AS gid, COUNT(like_feas) AS like_num;*/

--TODO: only use show and click here
joined = JOIN show BY gid LEFT OUTER, click by gid;
fea_joined = JOIN features_distinct BY id, joined BY $0;
/* joined = JOIN show RIGHT OUTER BY gid, click RIGHT OUTER BY gid, 
                unclick RIGHT OUTER BY gid, 
                like BY gid, features_distinct BY id; */
--following add the features:
fea_joined_with_max_feas = CROSS fea_joined, max_feas;
out = FOREACH fea_joined_with_max_feas GENERATE 
    (click_num IS NULl ? -1 : 1),
    (float)uidint/max_uidint, 
    (float)sourceint/max_sourceint, 
    (float)length_title/max_length_title, 
    (float)length_desc/max_length_desc, 
    (float)length_content/max_length_content;

/*DUMP out;*/
STORE max_feas INTO '$max_feas_out' USING PigStorage(',');
STORE out INTO '$train_out' USING PigStorage(' ');
