select count(*) as total from tasks where householdId = 'hh_b190b6ff-232b-42df-86a4-d65c45106057';
select count(*) as active from tasks where householdId = 'hh_b190b6ff-232b-42df-86a4-d65c45106057' and isDeleted = 0;
select count(*) as deleted from tasks where householdId = 'hh_b190b6ff-232b-42df-86a4-d65c45106057' and isDeleted = 1;
select assignedUserId, count(*) from tasks where householdId = 'hh_b190b6ff-232b-42df-86a4-d65c45106057' and isDeleted = 0 group by assignedUserId order by count(*) desc;
