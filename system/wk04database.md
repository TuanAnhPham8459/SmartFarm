# Database Status Update
## Recent and current progress
* Case was redesigned base on instructor's feedback.
* New sensor was added to the scope to replace bad one: Air quality sensor [BME688](https://www.amazon.ca/Environmental-Multi-Function-Capability-Temperature-Atmospheric/dp/B0GF231DPN/ref=asc_df_B0GF231DPN?mcid=907e48d90e1e32ffb9d70dce8d61d63a&tag=googleshopc0c-20&linkCode=df0&hvadid=706827341480&hvpos=&hvnetw=g&hvrand=2611770365168115372&hvpone=&hvptwo=&hvqmt=&hvdev=c&hvdvcmdl=&hvlocint=&hvlocphy=9000990&hvtargid=pla-2469178637734&psc=1&hvocijid=2611770365168115372-B0GF231DPN-&hvexpln=0&gad_source=1).
* Currently the database is fetching realtime sensor data, alert, device's config, feedback, and user account. Further discussion about what should be next to be sent to database is ongoing.
* Fetches sensor logs with configurable limits and sorts by timestamp descending.
* Links database documents directly to authenticated user IDs and email/password or Google sign-in credentials.
* Leverages Firestore's built-in offline persistence and cache metadata checks. Caches user profile data locally so the app remains usable when offline.
## Problems and hyperlinks/urls to potential solutions
Daisy chained sensors will not work if keeping the SenseHat. Since we use all kind of sensors, SenseHat is redundant and can be removed.
## Financial
### Expenditures since previous report
New air sensor BME688: 25$
### Planned future expenses
Bolt and nut
